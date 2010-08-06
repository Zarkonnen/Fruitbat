package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.PageChange;
import com.metalbeetle.fruitbat.storage.Store;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

class MultiplexStore implements Store {
	static final String SLAVE_FAILURE = "SLAVE_FAILURE";

	final List<Store> stores;
	final ProgressMonitor pm;
	final HashMap<Integer, Document> idToDoc = new HashMap<Integer, Document>();
	final ArrayList<Document> docs = new ArrayList<Document>();
	final HashMap<Integer, Document> idToDeletedDoc = new HashMap<Integer, Document>();
	final ArrayList<Document> deletedDocs = new ArrayList<Document>();
	final BitSet storeEnabled;
	boolean masterIDUpdated = false;

	Store master() {
		return stores.get(0);
	}

	boolean allStoresEnabled() {
		boolean all = true;
		for (int i = 0; i < stores.size(); i++) { all = all && storeEnabled.get(i); }
		return all;
	}
	
	public MultiplexStore(List<Store> stores, ProgressMonitor pm) throws FatalStorageException {
		pm.showProgressBar("Loading Multiplex Store", "", -1);
		try {
			this.stores = immute(stores);
			this.pm = pm;
			storeEnabled = new BitSet(stores.size());
			for (int i = 0; i < stores.size(); i++) { storeEnabled.set(i); }
			synchronize();
			for (Document d : master().docs()) {
				Document myD = new MultiplexDocument(d, this);
				idToDoc.put(myD.getID(), myD);
				docs.add(myD);
			}
			for (Document d : master().deletedDocs()) {
				Document myD = new MultiplexDocument(d, this);
				idToDeletedDoc.put(myD.getID(), myD);
				deletedDocs.add(myD);
			}
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		} finally {
			pm.hideProgressBar();
		}
	}

	int maxID(Store s) throws FatalStorageException {
		return Math.max(
				s.docs().size() > 0 ? s.docs().get(0).getID() : -1,
				s.deletedDocs().size() > 0 ? s.deletedDocs().get(0).getID() : -1
				);
	}

	void synchronize() throws FatalStorageException {
		pm.showProgressBar("Synchronizing stores", "", stores.size());
		try {
			List<Document> masterDocs = master().docs();
			List<Document> masterDeletedDocs = master().deletedDocs();
			if (masterDocs.size() == 0 && masterDeletedDocs.size() == 0) {
				if (stores.get(1).docs().size() > 0 || stores.get(1).deletedDocs().size() > 0) {
					pm.showWarning("storeRecovery", "Recovering data",
							"Since the master store is empty, Fruitbat will now restore it from " +
							"backup.");
					syncStores(stores.get(1), master(), -1);
				}
			}
			for (int i = 1; i < stores.size(); i++) {
				pm.progress("Backup " + stores.get(i), i);
				try {
					syncStores(master(), stores.get(i), i);
				} catch (FatalStorageException e) {
					handleSlaveStorageException(i, e);
				}
			}
		} finally {
			pm.hideProgressBar();
		}
	}

	void syncStores(Store from, Store to, int storeIndex) throws FatalStorageException {
		if (from.hasMetaData(MASTER_ID_KEY) && to.hasMetaData(MASTER_ID_KEY)) {
			if (!from.getMetaData(MASTER_ID_KEY).equals(to.getMetaData(MASTER_ID_KEY))) {
				throw new FatalStorageException("Cannot synchronize stores from " + from + " to " +
						to + ". The latter store is a backup for a different store, and " +
						"synchronizing them would overwrite that backup!");
			}
		}

		// Make sure the stores have the same number of documents.
		// Now ensure that the documents have the same level of deletedness.
		for (Document d : from.docs()) {
			if (to.get(d.getID()) == null) {
				to.getCreateOrUndelete(d.getID());
			}
		}
		for (Document d : from.deletedDocs()) {
			if (to.getDeleted(d.getID()) == null) {
				if (to.get(d.getID()) == null) {
					to.getCreateOrUndelete(d.getID());
				}
				to.delete(to.get(d.getID()));
			}
		}
		// Oh, and delete any documents that aren't supposed to be there.
		for (Document d2 : to.docs()) {
			if (from.get(d2.getID()) == null) {
				to.delete(d2);
			}
		}
		// Now ensure they have the same data.
		for (Document d : from.docs()) {
			Document d2 = to.get(d.getID());
			syncDocs(d, d2, storeIndex);
		}
		for (Document d : from.deletedDocs()) {
			Document d2 = to.getDeleted(d.getID());
			syncDocs(d, d2, storeIndex);
		}
	}

	void syncDocs(Document from, Document to, int i) throws FatalStorageException {
		if (!from.get(Document.CHANGE_ID_KEY).equals(to.get(Document.CHANGE_ID_KEY))) {
			pm.progress("Synchronizing " + from, i);
			ArrayList<Change> syncChanges = new ArrayList<Change>();
			// First, the data changes.
			// Changed and added keys.
			for (String key : from.keys()) {
				if (!to.has(key) || !from.get(key).equals(to.get(key))) {
					syncChanges.add(DataChange.put(key, from.get(key)));
				}
			}
			// Removed keys.
			for (String key : to.keys()) {
				if (!from.has(key)) {
					syncChanges.add(DataChange.remove(key));
				}
			}
			// Now the file changes.
			// Changed and added pages.
			HashSet<File> toDelete = new HashSet<File>();
			for (String key : from.pageKeys()) {
				if (!to.hasPage(key) || !from.getPageChecksum(key).equals(to.getPageChecksum(key))) {
					URI uri = from.getPage(key);
					File pageF;
					if (uri.getScheme().equals("file")) {
						pageF = new File(uri.getPath());
					} else {
						try {
							String name = uri.getPath().replaceAll("[^a-zA-Z0-9.]", "");
							download(uri.toURL(), pageF = File.createTempFile("", name));
							toDelete.add(pageF);
						} catch (Exception e) {
							throw new FatalStorageException("Cannot read page file at " + uri + ".",
									e);
						}
					}
					syncChanges.add(PageChange.put(key, pageF));
				}
			}
			for (String key : to.pageKeys()) {
				if (!from.hasPage(key)) {
					syncChanges.add(PageChange.remove(key));
				}
			}

			to.change(from.get(Document.CHANGE_ID_KEY), syncChanges);
			for (File f : toDelete) { f.delete(); }
		}
	}

	public Document create() throws FatalStorageException {
		try {
			Document d = master().create();
			MultiplexDocument md = new MultiplexDocument(d, this);
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						syncDocs(d, stores.get(i).getCreateOrUndelete(d.getID()), i);
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

	public Document getCreateOrUndelete(int id) throws FatalStorageException {
		try {
			if (idToDoc.containsKey(id)) { return idToDoc.get(id); }
			if (idToDeletedDoc.containsKey(id)) { return undelete(id); }
			Document d = master().getCreateOrUndelete(id);
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						syncDocs(d, stores.get(i).getCreateOrUndelete(id), i);
					} catch (FatalStorageException e) {
						handleSlaveStorageException(i, e);
					}
				}
			}
			MultiplexDocument md = new MultiplexDocument(d, this);
			idToDoc.put(md.getID(), md);
			docs.add(md);
			return md;
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Could not get/create/delete a document from the " +
					"master store.", e);
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
						syncDocs(d, stores.get(i).undelete(docID), i);
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
	public List<Document> deletedDocs() { return immute(deletedDocs); }

	public Document get(int id) { return idToDoc.get(id); }
	public Document getDeleted(int id) { return idToDeletedDoc.get(id); }

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
				"The backup store gave the following reason for its failure:<br>" +
				e.getFullMessage().replace("\n", "<br>"));
		storeEnabled.clear(slaveIndex);
	}

	@Override
	public String toString() { return "Multiplex Store of " + Arrays.toString(stores.toArray()); }

	public String getMetaData(String key) throws FatalStorageException {
		return master().getMetaData(key);
	}

	public void changeMetaData(List<Change> changes) throws FatalStorageException {
		try {
			master().changeMetaData(changes);
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					try {
						stores.get(i).changeMetaData(changes);
					} catch (FatalStorageException e) {
						handleSlaveStorageException(i, e);
					}
				}
			}
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	public boolean hasMetaData(String key) throws FatalStorageException {
		return master().hasMetaData(key);
	}

	void updateMasterID() throws FatalStorageException {
		if (!masterIDUpdated) {
			changeMetaData(l(DataChange.put(MASTER_ID_KEY,
					master().getMetaData(MASTER_ID_KEY))));
			masterIDUpdated = true;
		}
	}
}
