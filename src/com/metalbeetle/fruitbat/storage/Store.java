package com.metalbeetle.fruitbat.storage;

import java.util.List;

/** Interface for storing documents. */
public interface Store {
	/** This needs to be set to an unique string at store creation time. */
	public String getUUID() throws FatalStorageException;
	/**
	 * This should be set to a new value the first time a document in the store gets updated or
	 * a metadata key is changed in a given session. It should be set at store creation.
	 */
	public String getRevision() throws FatalStorageException;
	/** True when store is freshly created, false as soon as the revision changes the first time. */
	public boolean isEmptyStore() throws FatalStorageException;

	public void setProgressMonitor(ProgressMonitor pm);

	public int getNextRetainedPageNumber() throws FatalStorageException;
	public void setNextRetainedPageNumber(int nextRetainedPageNumber) throws FatalStorageException;
	public DocIndex getIndex();
	public DocIndex getDeletedIndex();
	public void close() throws FatalStorageException;

	public Document create() throws FatalStorageException;
	public Document getCreateOrUndelete(int id) throws FatalStorageException;
	public void delete(Document d) throws FatalStorageException;
	public Document undelete(int docID) throws FatalStorageException;
	public List<Document> docs() throws FatalStorageException;
	public List<Document> deletedDocs() throws FatalStorageException;
	public Document get(int id) throws FatalStorageException;
	public Document getDeleted(int id) throws FatalStorageException;

	public String getMetaData(String key) throws FatalStorageException;
	public void changeMetaData(List<Change> changes) throws FatalStorageException;
	public boolean hasMetaData(String key) throws FatalStorageException;
	public List<String> metaDataKeys() throws FatalStorageException;
}
