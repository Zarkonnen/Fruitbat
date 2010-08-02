package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.Store;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

class MultiplexStore implements Store {
	static final String SLAVE_FAILURE = "SLAVE_FAILURE";

	final List<Store> stores;
	final ProgressMonitor pm;
	final HashMap<Integer, Document> idToDoc = new HashMap<Integer, Document>();
	final ArrayList<Document> docs = new ArrayList<Document>();
	final BitSet storeEnabled;

	Store master() {
		return stores.get(0);
	}

	boolean allStoresEnabled() {
		boolean all = true;
		for (int i = 0; i < stores.size(); i++) { all = all && storeEnabled.get(i); }
		return all;
	}
	
	public MultiplexStore(List<Store> stores, ProgressMonitor pm) throws FatalStorageException {
		try {
			this.stores = immute(stores);
			this.pm = pm;
			storeEnabled = new BitSet(stores.size());
			for (int i = 0; i < stores.size(); i++) { storeEnabled.set(i); }
			for (Document d : master().docs()) {
				Document myD = new MultiplexDocument(d, this);
				idToDoc.put(myD.getID(), myD);
				docs.add(myD);
			}
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	public Document create() throws FatalStorageException {
		try {
			Document d = master().create();
			MultiplexDocument md = new MultiplexDocument(d, this);
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						stores.get(i).create();
					} catch (FatalStorageException e) {
						handleSlaveStorageException(i, e);
					}
				}
			}
			idToDoc.put(md.getID(), md);
			docs.add(md);
			return md;
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Could not create a new document in the master store.",
					e);
		}
	}

	public void delete(Document d) throws FatalStorageException {
		try {
			int id = d.getID();
			master().delete(master().get(id));
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						stores.get(i).delete(stores.get(i).get(id));
					} catch (FatalStorageException e) {
						handleSlaveStorageException(i, e);
					}
				}
			}
			idToDoc.remove(id);
			docs.remove(d);
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Could not delete a document from the master store.",
					e);
		}
	}

	public Document undelete(int docID) throws FatalStorageException {
		try {
			Document d = master().undelete(docID);
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						stores.get(i).undelete(docID);
					} catch (FatalStorageException e) {
						handleSlaveStorageException(i, e);
					}
				}
			}
			Document md =  new MultiplexDocument(d, this);
			idToDoc.put(md.getID(), md);
			docs.add(md);
			return md;
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Could not undelete a document from the master store.",
					e);
		}
	}

	public List<Document> docs() { return immute(docs); }

	public Document get(int id) { return idToDoc.get(id); }

	public void setProgressMonitor(ProgressMonitor pm) {
		for (Store s : stores) { s.setProgressMonitor(pm); }
	}

	public int getNextRetainedPageNumber() throws FatalStorageException {
		try {
			return master().getNextRetainedPageNumber();
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	public void setNextRetainedPageNumber(int nextRetainedPageNumber) throws FatalStorageException {
		try {
			master().setNextRetainedPageNumber(nextRetainedPageNumber);
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						stores.get(i).setNextRetainedPageNumber(nextRetainedPageNumber);
					} catch (FatalStorageException e) {
						handleSlaveStorageException(i, e);
					}
				}
			}
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	public void close() throws FatalStorageException {
		try {
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						stores.get(i).close();
					} catch (FatalStorageException e) {
						handleSlaveStorageException(i, e);
					}
				}
			}
			master().close();
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Unable to cleanly close connection to master store.",
					e);
		}
	}

	public DocIndex getIndex() { return stores.get(0).getIndex(); }

	void handleSlaveStorageException(int slaveIndex, FatalStorageException e) {
		pm.showWarning(SLAVE_FAILURE, "Backup store " + stores.get(slaveIndex) + " disabled",
				"<html>Unable to communicate with backup store " + stores.get(slaveIndex) +
				". You can continue working, and your changes will be pushed into the backup " +
				"when communication is restored.<br><br>" +
				"(The backup store gave the following reason for its failure:<br>" +
				e.getFullMessage().replace("\n", "<br>"));
		storeEnabled.clear(slaveIndex);
	}
}
