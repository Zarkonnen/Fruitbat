package com.metalbeetle.fruitbat.hierarchicalstorage;

import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.Collection;
import java.util.List;

/** Stores key/value data. */
public interface KVFile {
	public boolean has(String key) throws FatalStorageException;
	public String get(String key) throws FatalStorageException;
	public Collection<String> keys() throws FatalStorageException;
	public void change(List<Change> changes) throws FatalStorageException;
	public void saveToCache() throws FatalStorageException;
}
