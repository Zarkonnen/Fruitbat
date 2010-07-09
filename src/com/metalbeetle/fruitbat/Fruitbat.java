package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.atrstorage.ATRDocIndex;
import com.metalbeetle.fruitbat.atrstorage.ATRStore;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Store;
import java.io.File;

/** Application instance. */
public class Fruitbat {
	private final Store store;
	private final DocIndex index;

	public DocIndex getIndex() { return index; }
	public Store getStore() { return store; }

	public Fruitbat(File storeLocation) {
		store = new ATRStore(storeLocation);
		index = new ATRDocIndex((ATRStore) store);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				close();
			}
		});
	}

	public void close() { index.close(); store.close(); }
}
