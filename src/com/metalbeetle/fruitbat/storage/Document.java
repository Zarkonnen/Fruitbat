package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.io.DataSrc;
import java.util.List;

/** Stores key/value string data and pages as files. */
public interface Document {
	public int getID() throws FatalStorageException;

	/** @return The ID of the current revision */
	public String getRevision() throws FatalStorageException;
	
	public String change(String changeID, List<Change> changes) throws FatalStorageException;
	public String change(List<Change> changes) throws FatalStorageException;

	public boolean has(String key) throws FatalStorageException;
	public String get(String key) throws FatalStorageException;
	public List<String> keys() throws FatalStorageException;

	public boolean hasPage(String key) throws FatalStorageException;
	public DataSrc getPage(String key) throws FatalStorageException;
	public String getPageChecksum(String key) throws FatalStorageException;
	public List<String> pageKeys() throws FatalStorageException;
}
