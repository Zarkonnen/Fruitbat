package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.IdentityHashMap;
import java.util.List;

class MultiplexDocument implements Document, Comparable<MultiplexDocument> {
	final IdentityHashMap<EnhancedStore, Document> docs;
	final MultiplexStore s;
	final int id;

	Document master() {
		return docs.get(s.master());
	}

	Document fastest() {
		return docs.get(s.fastest());
	}

	MultiplexDocument(IdentityHashMap<EnhancedStore, Document> docs, MultiplexStore s) throws FatalStorageException {
		this.docs = docs;
		this.s = s;
		id = master().getID();
	}
	
	public int getID() { return id; }

	public String change(List<? extends Change> changes) throws FatalStorageException {
		String changeID;
		try {
			changeID = master().change(changes);
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Unable to write data to the master document store.",
					e);
		}
		changeSlaves(changeID, changes);
		s.updateStoredRevisions();
		return changeID;
	}
	
	public String change(String changeID, List<? extends Change> changes) throws FatalStorageException {
		try {
			master().change(changeID, changes);
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Unable to write data to the master document store.",
					e);
		}
		changeSlaves(changeID, changes);
		s.updateStoredRevisions();
		return changeID;
	}
	
	private void changeSlaves(String changeID, List<? extends Change> changes) throws FatalStorageException {
		for (int slaveIndex = 1; slaveIndex < s.stores.size(); slaveIndex++) {
			if (s.storeEnabled.get(slaveIndex)) {
				try {
					docs.get(s.stores.get(slaveIndex)).change(changeID, changes);
				} catch (FatalStorageException e) {
					s.handleStorageException(s.stores.get(slaveIndex), e);
				}
			}
		}
	}

	public String getRevision() throws FatalStorageException {
		while (true) {
			try {
				return fastest().getRevision();
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public boolean has(String key) throws FatalStorageException {
		while (true) {
			try {
				return fastest().has(key);
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public String get(String key) throws FatalStorageException {
		while (true) {
			try {
				return fastest().get(key);
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public List<String> keys() throws FatalStorageException {
		while (true) {
			try {
				return fastest().keys();
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}
	
	public boolean hasPage(String key) throws FatalStorageException {
		while (true) {
			try {
				return fastest().hasPage(key);
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public DataSrc getPage(String key) throws FatalStorageException {
		while (true) {
			try {
				return fastest().getPage(key);
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public String getPageChecksum(String key) throws FatalStorageException {
		while (true) {
			try {
				return fastest().getPageChecksum(key);
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public List<String> pageKeys() throws FatalStorageException {
		while (true) {
			try {
				return fastest().pageKeys();
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	@Override
	public String toString() {
		return "Multiplexed document with ID " + id;
	}

	public int compareTo(MultiplexDocument d2) {
		return id - d2.id;
	}
}
