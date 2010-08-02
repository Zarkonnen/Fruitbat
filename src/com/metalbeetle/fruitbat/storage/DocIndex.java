package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.Closeable;
import java.util.List;
import java.util.Map;

public interface DocIndex extends Closeable {
	public static final int ALL_DOCS = -1;

	public SearchResult search(Map<String, String> searchKV, int maxDocs) throws FatalStorageException;

	/** @return Whether the given key is featured in the document set. */
	public boolean isKey(String key) throws FatalStorageException;

	/** @return All keys in the document set. */
	public List<String> allKeys() throws FatalStorageException;
}
