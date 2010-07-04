package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.atrstorage.ATRKeyIndex;
import com.metalbeetle.fruitbat.atrstorage.ATRStore;
import com.metalbeetle.fruitbat.storage.KeyIndex;
import com.metalbeetle.fruitbat.storage.Store;
import java.io.File;

/** Application instance. */
public class Fruitbat {
	private final Store store;
	private final KeyIndex index;

	public KeyIndex getIndex() { return index; }
	public Store getStore() { return store; }

	public Fruitbat(File storeLocation) {
		store = new ATRStore(storeLocation);
		index = new ATRKeyIndex((ATRStore) store);
	}

	public void close() { index.close(); }
}
