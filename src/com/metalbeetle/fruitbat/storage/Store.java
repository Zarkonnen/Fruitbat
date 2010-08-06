package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.Fruitbat;
import java.util.List;

/** Interface for storing documents. */
public interface Store {
	/**
	 * This should be set to a random value the first time a document in the store gets updated in
	 * a given session.
	 */
	static final String MASTER_ID_KEY =
			Fruitbat.HIDDEN_KEY_PREFIX + Fruitbat.HIDDEN_KEY_PREFIX + "mid";

	public Document create() throws FatalStorageException;
	public Document getCreateOrUndelete(int id) throws FatalStorageException;
	public void delete(Document d) throws FatalStorageException;
	public Document undelete(int docID) throws FatalStorageException;
	public List<Document> docs() throws FatalStorageException;
	public List<Document> deletedDocs() throws FatalStorageException;
	public Document get(int id) throws FatalStorageException;
	public Document getDeleted(int id) throws FatalStorageException;
	public void setProgressMonitor(ProgressMonitor pm);
	public int getNextRetainedPageNumber() throws FatalStorageException;
	public void setNextRetainedPageNumber(int nextRetainedPageNumber) throws FatalStorageException;
	public DocIndex getIndex();
	public void close() throws FatalStorageException;
	public String getMetaData(String key) throws FatalStorageException;
	public void changeMetaData(List<Change> changes) throws FatalStorageException;
	public boolean hasMetaData(String key) throws FatalStorageException;
}
