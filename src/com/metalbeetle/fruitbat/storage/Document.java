package com.metalbeetle.fruitbat.storage;

import java.net.URI;
import java.util.List;

/** Stores key/value string data and pages as files. */
public interface Document {
	public int getID() throws FatalStorageException;
	public long getVersion() throws FatalStorageException;

	public void change(List<Change> changes) throws FatalStorageException;

	public boolean has(String key) throws FatalStorageException;
	public String get(String key) throws FatalStorageException;
	public List<String> keys() throws FatalStorageException;

	public boolean hasPage(String key) throws FatalStorageException;
	public URI getPage(String key) throws FatalStorageException;
	public List<String> pageKeys() throws FatalStorageException;
}
