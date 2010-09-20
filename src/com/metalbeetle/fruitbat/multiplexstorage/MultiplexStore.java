package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.io.DataSrc;
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

	// The multiplex store stores the slaves' revisions in metadata keyed by the slaves' UUID, so it
	// can tell when the slaves have been modified independently.
	static final String SLAVE_REVISION_KEY_PREFIX =
			Fruitbat.HIDDEN_KEY_PREFIX + Fruitbat.HIDDEN_KEY_PREFIX + "slave rev for ";
	static final String SLAVE_REVISION_KEY_INFIX = " of ";

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
	
	public MultiplexStore(List<Store> stores, ProgressMonitor pm) throws FatalStorageException {
		pm.showProgressBar("Loading Multiplex Store", "", -1);
		try {
			this.stores = immute(stores);
			this.pm = pm;
			// Set up bit set to track which stores are enabled.
			storeEnabled = new BitSet(stores.size());
			for (int i = 0; i < stores.size(); i++) { storeEnabled.set(i); }
			// Synchronize master store data to slaves. (Or download slave data into empty master.)
			synchronize();
			// Create MultiplexDocuments
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

	void synchronize() throws FatalStorageException {
		pm.showProgressBar("Synchronizing stores", "", stores.size());
		try {
			if (master().isEmptyStore() && !stores.get(1).isEmptyStore()) {
					pm.showWarning("storeRecovery", "Recovering data",
							"Since the master store is empty, Fruitbat will now restore it from " +
							"backup.");
					syncStores(stores.get(1), master(), -1, /*secondStoreIsBackupOfFirst*/ false);
			}
			for (int i = 1; i < stores.size(); i++) {
				pm.progress("Backup " + stores.get(i), i);
				try {
					syncStores(master(), stores.get(i), i, /*secondStoreIsBackupOfFirst*/ true);
				} catch (FatalStorageException e) {
					handleSlaveStorageException(i, e);
				}
			}
		} finally {
			pm.hideProgressBar();
		}
	}

	static String getSlaveRevKey(Store master, Store slave) throws FatalStorageException {
		return SLAVE_REVISION_KEY_PREFIX + slave.getUUID() + SLAVE_REVISION_KEY_INFIX +
				master.getUUID();
	}

	void syncStores(Store from, Store to, int storeIndex, boolean secondStoreIsBackupOfFirst)
			throws FatalStorageException
	{
		// If "to" is a slave store of "from", check that the revision "from" has for "to" is
		// unchanged - otherwise someone modified the backup independently, which is BAD.
		String slaveRevKey = getSlaveRevKey(from, to);
		if (secondStoreIsBackupOfFirst &&
		    !to.isEmptyStore() &&
		    from.hasMetaData(slaveRevKey) &&
		    !from.getMetaData(slaveRevKey).equals(to.getRevision()))
		{
			throw new FatalStorageException("Cannot use store " + to + " as a backup for store " +
					from + ". It has been modified independently, and these modifications would " +
					"be overwritten.");
		}

		syncMetaData(from, to);

		// Ensure that the stores have the same docs and deleted docs.
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
		// And delete any documents that aren't supposed to be there. This isn't perfect, as it will
		// still leave a residue of deleted documents, but at least it's usually hidden from the
		// user.
		for (Document d2 : to.docs()) {
			if (from.get(d2.getID()) == null) {
				to.delete(d2);
			}
		}
		// Now ensure the documents have the same data.
		for (Document d : from.docs()) {
			syncDocs(d, to.get(d.getID()), storeIndex);
		}
		for (Document d : from.deletedDocs()) {
			syncDocs(d, to.getDeleted(d.getID()), storeIndex);
		}

		// If there is a master/slave thing going on, store "to"'s revision in the master's
		// metadata, then re-sync the metadata for consistency.
		if (secondStoreIsBackupOfFirst && !to.isEmptyStore()) {
			from.changeMetaData(l(DataChange.put(slaveRevKey, to.getRevision())));
		}
		syncMetaData(from, to);
	}

	void syncMetaData(Store from, Store to) throws FatalStorageException {
		pm.progress("Synchronizing metadata", -1);
		ArrayList<Change> syncChanges = new ArrayList<Change>();
		// Changed and added keys.
		for (String key : from.metaDataKeys()) {
			if (!to.hasMetaData(key) || !from.getMetaData(key).equals(to.getMetaData(key))) {
				syncChanges.add(DataChange.put(key, from.getMetaData(key)));
			}
		}
		// Removed keys.
		for (String key : to.metaDataKeys()) {
			if (!from.hasMetaData(key)) {
				syncChanges.add(DataChange.remove(key));
			}
		}
		if (syncChanges.size() > 0) {
			to.changeMetaData(syncChanges);
		}
	}

	void syncDocs(Document from, Document to, int i) throws FatalStorageException {
		if (!from.getRevision().equals(to.getRevision())) {
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
					syncChanges.add(PageChange.put(key, from.getPage(key)));
				}
			}
			for (String key : to.pageKeys()) {
				if (!from.hasPage(key)) {
					syncChanges.add(PageChange.remove(key));
				}
			}

			to.change(from.getRevision(), syncChanges);
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
				"Unable to communicate with backup store " + stores.get(slaveIndex) +
				". You can continue working, and your changes will be pushed into the backup " +
				"when communication is restored.\n\n" +
				"The backup store gave the following reason for its failure:\n" +
				getFullMessage(e));
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
			updateRevision();
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	public boolean hasMetaData(String key) throws FatalStorageException {
		return master().hasMetaData(key);
	}

	public List<String> metaDataKeys() throws FatalStorageException {
		try {
			return master().metaDataKeys();
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	void updateRevision() throws FatalStorageException {
		if (!masterIDUpdated) {
			masterIDUpdated = true;
			// Store the revisions of all the slaves in the master's metadata for coherency checking
			// purposes.
			ArrayList<Change> mdc = new ArrayList<Change>();
			for (int i = 1; i < stores.size(); i++) {
				if (storeEnabled.get(i)) {
					Store slave = stores.get(i);
					mdc.add(DataChange.put(
							getSlaveRevKey(master(), slave),
							slave.getRevision()));
				}
			}
			changeMetaData(mdc);
		}
	}

	public String getUUID() throws FatalStorageException {
		try {
			return master().getUUID();
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	public String getRevision() throws FatalStorageException {
		try {
			return master().getRevision();
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}

	public boolean isEmptyStore() throws FatalStorageException {
		try {
			return master().isEmptyStore();
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Cannot communicate with master store.", e);
		}
	}
}
