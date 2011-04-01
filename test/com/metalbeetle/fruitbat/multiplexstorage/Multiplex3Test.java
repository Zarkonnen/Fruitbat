package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.filestorage.ATRStorageSystem;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class Multiplex3Test {
	File sf1;
	File sf2;
	EnhancedStore s1;
	EnhancedStore s2;
	EnhancedStore ms;

	@Test
	/**
	 * This tests for a subtle bug where the revision of a slave store stored in the master's
	 * metadata lagged behing the slave's by one.
	 */
	public void revisionConsistentOnOpenAndClose() throws Exception {
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		try {
			StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
			StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
			StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
			ms = msc.init(new DummyProgressMonitor());
			ms.close();
			ms = msc.init(new DummyProgressMonitor());
			assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
			Document d = ms.create(); // This bumps up the revision to 1.
			ms.close();
			// This causes a sync that bumps up the revision to 2, but leaves the stored rev at 1.
			ms = msc.init(new DummyProgressMonitor());
			assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
			ms.close();
			// This crashes due to a revision mismatch.
			ms = msc.init(new DummyProgressMonitor());
			assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		} finally {
			Util.deleteRecursively(sf1);
			Util.deleteRecursively(sf2);
		}
	}

	@Test
	public void correctRevisionStoredAfterOperation() throws FatalStorageException {
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		try {
			StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
			StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
			StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
			ms = msc.init(new DummyProgressMonitor());
			int id = ms.create().getID();
			assertEquals(
					((MultiplexStore) ms.s).stores.get(1).getRevision(),
					((MultiplexStore) ms.s).getMetaData(MultiplexStore.getSlaveRevKey(
						((MultiplexStore) ms.s).stores.get(0), ((MultiplexStore) ms.s).stores.get(1))));
			ms.close();
			ms = msc.init(new DummyProgressMonitor());
			Document d = ms.get(id);
			d.change(l(DataChange.put("a", "b")));
			assertEquals(
					((MultiplexStore) ms.s).stores.get(1).getRevision(),
					((MultiplexStore) ms.s).getMetaData(MultiplexStore.getSlaveRevKey(
						((MultiplexStore) ms.s).stores.get(0), ((MultiplexStore) ms.s).stores.get(1))));
			ms.close();
			ms = msc.init(new DummyProgressMonitor());
			ms.changeMetaData(l(DataChange.put("foo", "bar")));
			assertEquals(
					((MultiplexStore) ms.s).stores.get(1).getRevision(),
					((MultiplexStore) ms.s).getMetaData(MultiplexStore.getSlaveRevKey(
						((MultiplexStore) ms.s).stores.get(0), ((MultiplexStore) ms.s).stores.get(1))));
			ms.close();
			ms = msc.init(new DummyProgressMonitor());
			ms.delete(ms.get(id));
			assertEquals(
					((MultiplexStore) ms.s).stores.get(1).getRevision(),
					((MultiplexStore) ms.s).getMetaData(MultiplexStore.getSlaveRevKey(
						((MultiplexStore) ms.s).stores.get(0), ((MultiplexStore) ms.s).stores.get(1))));
			ms.close();
			ms = msc.init(new DummyProgressMonitor());
			ms.undelete(ms.get(id));
			assertEquals(
					((MultiplexStore) ms.s).stores.get(1).getRevision(),
					((MultiplexStore) ms.s).getMetaData(MultiplexStore.getSlaveRevKey(
						((MultiplexStore) ms.s).stores.get(0), ((MultiplexStore) ms.s).stores.get(1))));
			ms.close();
		} finally {
			Util.deleteRecursively(sf1);
			Util.deleteRecursively(sf2);
		}
	}
}
