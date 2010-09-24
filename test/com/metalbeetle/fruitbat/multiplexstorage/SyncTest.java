package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.atrstorage.ATRStorageSystem;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.io.FileSrc;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.PageChange;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class SyncTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";
	
	File sf1;
	File sf2;
	Store s1;
	Store s2;
	EnhancedStore ms;
	File p;
	DataSrc pSrc;

	@Test
	public void testPutSync() throws FatalStorageException, StoreConfigInvalidException, IOException, Exception {
		p = Util.createFile("foobar");
		pSrc = new FileSrc(p);
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		s1 = sc1.init(new DummyProgressMonitor());
		Document d = s1.create();
		int id = d.getID();
		d.change(l(DataChange.put("key", "value"), PageChange.put("page", pSrc)));
		s1.close();
		ms = msc.init(new DummyProgressMonitor());
		d = ms.get(id);
		assertEquals("value", d.get("key"));
		assertTrue(Util.hasFirstLine(d.getPage("page"), "foobar"));
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();
		s2 = sc2.init(new DummyProgressMonitor());
		d = s2.get(id);
		assertEquals("value", d.get("key"));
		assertTrue(Util.hasFirstLine(d.getPage("page"), "foobar"));
		s2.close();
		// Start up ms again to ensure master ID is coherent.
		ms = msc.init(new DummyProgressMonitor());
		d = ms.get(id);
		assertEquals("value", d.get("key"));
		assertTrue(Util.hasFirstLine(d.getPage("page"), "foobar"));
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();
		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void testChangeAndRemoveSync() throws FatalStorageException, StoreConfigInvalidException, IOException, Exception {
		p = Util.createFile("foobar");
		pSrc = new FileSrc(p);
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		s1 = sc1.init(new DummyProgressMonitor());
		Document d = s1.create();
		int id = d.getID();
		d.change(l(DataChange.put("key", "value"), DataChange.put("key2", "value"), PageChange.put("page", pSrc)));
		d.change(l(DataChange.put("key", "value2"), DataChange.remove("key2"), PageChange.move("page", "page2")));
		s1.close();
		ms = msc.init(new DummyProgressMonitor());
		d = ms.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertTrue(Util.hasFirstLine(d.getPage("page2"), "foobar"));
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();
		s2 = sc2.init(new DummyProgressMonitor());
		d = s2.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertTrue(Util.hasFirstLine(d.getPage("page2"), "foobar"));
		s2.close();
		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void testPulldownFromBackup() throws FatalStorageException, StoreConfigInvalidException, IOException, Exception {
		p = Util.createFile("foobar");
		pSrc = new FileSrc(p);
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		s2 = sc2.init(new DummyProgressMonitor());
		Document d = s2.create();
		int id = d.getID();
		d.change(l(DataChange.put("key", "value"), DataChange.put("key2", "value"), PageChange.put("page", pSrc)));
		d.change(l(DataChange.put("key", "value2"), DataChange.remove("key2"), PageChange.move("page", "page2")));
		s2.close();
		ms = msc.init(new DummyProgressMonitor());
		d = ms.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertTrue(Util.hasFirstLine(d.getPage("page2"), "foobar"));
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();
		s1 = sc2.init(new DummyProgressMonitor());
		d = s1.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertTrue(Util.hasFirstLine(d.getPage("page2"), "foobar"));
		s1.close();
		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void testRefuseToOverwriteBackup() throws FatalStorageException, StoreConfigInvalidException, IOException {
		p = Util.createFile("foobar");
		pSrc = new FileSrc(p);
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		ms = msc.init(new DummyProgressMonitor());
		Document d = ms.create();
		d.change(l(DataChange.put("key", "value"), DataChange.put("key2", "value"), PageChange.put("page", pSrc)));
		d.change(l(DataChange.put("key", "value2"), DataChange.remove("key2"), PageChange.move("page", "page2")));
		ms.close();
		s2 = sc2.init(new DummyProgressMonitor());
		d = s2.create();
		d.change(l(DataChange.put("spoiler", "spoils the master ID!")));
		s2.close();
		ms = msc.init(new DummyProgressMonitor());
		assertFalse(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();
		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void testDoNothingInMaster() throws FatalStorageException, StoreConfigInvalidException, IOException, InterruptedException {
		p = Util.createFile("foobar");
		pSrc = new FileSrc(p);
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		ms = msc.init(new DummyProgressMonitor());
		Document d = ms.create();
		d.change(l(DataChange.put("key", "value"), DataChange.put("key2", "value"), PageChange.put("page", pSrc)));
		ms.close();

		ms = msc.init(new DummyProgressMonitor());
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();

		ms = msc.init(new DummyProgressMonitor());
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();

		ms = msc.init(new DummyProgressMonitor());
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();

		ms = msc.init(new DummyProgressMonitor());
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();

		ms = msc.init(new DummyProgressMonitor());
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();

		Thread.sleep(1200);

		ms = msc.init(new DummyProgressMonitor());
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();

		ms = msc.init(new DummyProgressMonitor());
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();

		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void allowSyncToPreExistingBackup() throws FatalStorageException, StoreConfigInvalidException, IOException {
		p = Util.createFile("foobar");
		pSrc = new FileSrc(p);
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		ms = msc.init(new DummyProgressMonitor());
		Document d = ms.create();
		int id = d.getID();
		d.change(l(DataChange.put("key", "value"), DataChange.put("key2", "value"), PageChange.put("page", pSrc)));
		d.change(l(DataChange.put("key", "value2"), DataChange.remove("key2"), PageChange.move("page", "page2")));
		ms.close();
		s1 = sc2.init(new DummyProgressMonitor());
		d = s1.get(id);
		d.change(l(DataChange.put("another", "change")));
		s1.close();
		ms = msc.init(new DummyProgressMonitor());
		assertFalse(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();
		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void pulldownMetaDataFromBackup() throws FatalStorageException, StoreConfigInvalidException {
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		s2 = sc2.init(new DummyProgressMonitor());
		s2.changeMetaData(l(DataChange.put(SCARY, SCARY), DataChange.put(SCARY + "2", SCARY)));
		s2.changeMetaData(l(DataChange.remove(SCARY + "2")));
		s2.close();
		ms = msc.init(new DummyProgressMonitor());
		assertTrue(ms.hasMetaData(SCARY));
		assertFalse(ms.hasMetaData(SCARY + "2"));
		assertEquals(SCARY, ms.getMetaData(SCARY));
		assertTrue(((MultiplexStore) ms.s).storeEnabled.get(1));
		ms.close();
		s2 = sc2.init(new DummyProgressMonitor());
		assertTrue(s2.hasMetaData(SCARY));
		assertFalse(s2.hasMetaData(SCARY + "2"));
		assertEquals(SCARY, s2.getMetaData(SCARY));
		s2.close();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}
}