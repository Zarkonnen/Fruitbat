package com.metalbeetle.fruitbat.storage;

import java.util.List;

/** Interface for storing documents. */
public interface Store {
	public Document create() throws FatalStorageException;
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
}
