package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.Fruitbat;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

/**
 * Delegates calls to store and adds extra useful functionality.
 */
public class EnhancedStore implements Store {
	public final Store s;

	public EnhancedStore(Store s) { this.s = s; }

	public Document getCreateOrUndelete(int id) throws FatalStorageException {
		return undelete(getOrCreate(id));
	}

	/** Mark the document as deleted. */
	public Document delete(Document d) throws FatalStorageException {
		if (!d.has(Fruitbat.ALIVE_KEY)) { return d; }
		try {
			d.change(l(
					DataChange.put(Fruitbat.DEAD_KEY, ""),
					DataChange.remove(Fruitbat.ALIVE_KEY)));
			return d;
		} catch (Exception e) {
			throw new FatalStorageException("Could not delete document.", e);
		}
	}

	/** Mark the document as alive again. */
	public Document undelete(Document d) throws FatalStorageException {
		if (!d.has(Fruitbat.DEAD_KEY)) { return d; }
		try {
			d.change(l(
					DataChange.put(Fruitbat.ALIVE_KEY, ""),
					DataChange.remove(Fruitbat.DEAD_KEY)));
			return d;
		} catch (Exception e) {
			throw new FatalStorageException("Could not delete document.", e);
		}
	}

	// Autogenerated by NetBeans, delegating all methods in the Store interface to s.
	public void setProgressMonitor(ProgressMonitor pm) {
		s.setProgressMonitor(pm);
	}

	public void setNextRetainedPageNumber(int nextRetainedPageNumber) throws FatalStorageException {
		s.setNextRetainedPageNumber(nextRetainedPageNumber);
	}

	public List<String> metaDataKeys() throws FatalStorageException {
		return s.metaDataKeys();
	}

	public boolean isEmptyStore() throws FatalStorageException {
		return s.isEmptyStore();
	}

	public boolean hasMetaData(String key) throws FatalStorageException {
		return s.hasMetaData(key);
	}

	public String getUUID() throws FatalStorageException {
		return s.getUUID();
	}

	public String getRevision() throws FatalStorageException {
		return s.getRevision();
	}

	public int getNextRetainedPageNumber() throws FatalStorageException {
		return s.getNextRetainedPageNumber();
	}

	public String getMetaData(String key) throws FatalStorageException {
		return s.getMetaData(key);
	}

	public DocIndex getIndex() {
		return s.getIndex();
	}

	public Document get(int id) throws FatalStorageException {
		return s.get(id);
	}

	public List<Document> docs() throws FatalStorageException {
		return s.docs();
	}

	public Document create() throws FatalStorageException {
		return s.create();
	}

	public Document getOrCreate(int id) throws FatalStorageException {
		return s.getOrCreate(id);
	}

	public void close() throws FatalStorageException {
		s.close();
	}

	public void changeMetaData(List<Change> changes) throws FatalStorageException {
		s.changeMetaData(changes);
	}

	@Override
	public String toString() {
		return s.toString();
	}
}
