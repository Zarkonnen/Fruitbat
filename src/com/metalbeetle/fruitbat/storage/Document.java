package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.Fruitbat;
import java.net.URI;
import java.util.List;

/** Stores key/value string data and pages as files. */
public interface Document {
	public static final String CHANGE_ID_KEY =
			Fruitbat.HIDDEN_KEY_PREFIX + Fruitbat.HIDDEN_KEY_PREFIX + "cid";

	public int getID() throws FatalStorageException;
	public boolean isDeleted() throws FatalStorageException;

	public String change(String changeID, List<Change> changes) throws FatalStorageException;
	public String change(List<Change> changes) throws FatalStorageException;

	public boolean has(String key) throws FatalStorageException;
	public String get(String key) throws FatalStorageException;
	public List<String> keys() throws FatalStorageException;

	public boolean hasPage(String key) throws FatalStorageException;
	public URI getPage(String key) throws FatalStorageException;
	public String getPageChecksum(String key) throws FatalStorageException;
	public List<String> pageKeys() throws FatalStorageException;
}
