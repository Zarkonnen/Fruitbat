package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.List;

class MultiplexDocument implements Document, Comparable<MultiplexDocument> {
	final Document master;
	final MultiplexStore s;
	final int id;

	MultiplexDocument(Document master, MultiplexStore s) throws FatalStorageException {
		this.master = master;
		this.s = s;
		id = master.getID();
	}
	
	public int getID() { return id; }

	public String change(List<? extends Change> changes) throws FatalStorageException {
		String changeID;
		try {
			changeID = master.change(changes);
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Unable to write data to the master document store.",
					e);
		}
		changeSlaves(changeID, changes);
		s.updateRevision();
		return changeID;
	}
	
	public String change(String changeID, List<? extends Change> changes) throws FatalStorageException {
		try {
			master.change(changeID, changes);
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Unable to write data to the master document store.",
					e);
		}
		changeSlaves(changeID, changes);
		s.updateRevision();
		return changeID;
	}
	
	private void changeSlaves(String changeID, List<? extends Change> changes) {
		for (int slaveIndex = 1; slaveIndex < s.stores.size(); slaveIndex++) {
			if (s.storeEnabled.get(slaveIndex)) {
				try {
					Document d = s.stores.get(slaveIndex).get(id);
					if (d == null) {
						throw new FatalStorageException("Document with ID " + id + " could not " +
								"be found in " + s.stores.get(slaveIndex) + ".");
					}
					d.change(changeID, changes);
				} catch (FatalStorageException e) {
					s.handleSlaveStorageException(slaveIndex, e);
				}
			}
		}
	}

	public String getRevision() throws FatalStorageException { return master.getRevision(); }
	public boolean has(String key) throws FatalStorageException { return master.has(key); }
	public String get(String key) throws FatalStorageException { return master.get(key); }
	public List<String> keys() throws FatalStorageException { return master.keys(); }
	public boolean hasPage(String key) throws FatalStorageException { return master.hasPage(key); }
	public DataSrc getPage(String key) throws FatalStorageException { return master.getPage(key); }
	public String getPageChecksum(String key) throws FatalStorageException { return master.getPageChecksum(key); }
	public List<String> pageKeys() throws FatalStorageException { return master.pageKeys(); }

	@Override
	public String toString() {
		return "Multiplexed document with ID " + id;
	}

	public int compareTo(MultiplexDocument d2) {
		return id - d2.id;
	}
}
