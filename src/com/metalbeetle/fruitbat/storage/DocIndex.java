package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.gui.ProgressMonitor;
import java.util.Map;

public interface DocIndex {
	public static final int ALL_DOCS = -1;
	public static final int NO_TIMEOUT = -1;

	public SearchResult search(Map<String, String> searchKV, int maxDocs, int timeoutMs);

	/** @return Whether the given key is featured in the document set. */
	public boolean isKey(String key);

	/** Call this when you're done using the index! */
	public void close();
}
