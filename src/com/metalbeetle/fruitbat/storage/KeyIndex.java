package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.util.Pair;
import java.util.List;

public interface KeyIndex {
	/** @return List of matching documents and list of available narrowing search keys. */
	public Pair<List<Document>, List<String>> searchKeys(List<String> searchKeys);

	/** Call this when you're done using the index! */
	public void close();
}
