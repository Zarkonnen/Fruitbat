package com.metalbeetle.fruitbat.hierarchicalstorage;

import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.Collection;
import java.util.List;

/** Stores key/value data. */
public interface KVFile {
	/**
	 * @return Whether the file has a mapping for key.
	 */
	public boolean has(String key) throws FatalStorageException;
	/**
	 * @return The mapping for the given key.
	 * @throws FatalStorageException In case of error or if there is no mapping.
	 */
	public String get(String key) throws FatalStorageException;
	/** @return A collection of all the keys in the file. */
	public Collection<String> keys() throws FatalStorageException;
	/**
	 * @param changes A list of data changes to apply. File changes are not valid here.
	 */
	public void change(List<DataChange> changes) throws FatalStorageException;
	/**
	 * Hint to the KV file to flush its contents to any cache it may have.
	 */
	public void saveToCache() throws FatalStorageException;
}
