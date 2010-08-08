package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.atrstorage.ATRStorageSystem;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
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

public class MultiplexTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";
	
	File sf1;
	File sf2;
	Store s1;
	Store s2;
	Store ms;
	File p;

	@Test
	public void metaDataStoreAndRetrieve() throws FatalStorageException, StoreConfigInvalidException {
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		ms = msc.init(new DummyProgressMonitor());
		ms.changeMetaData(l(DataChange.put(SCARY, SCARY), DataChange.put(SCARY + "2", SCARY)));
		ms.changeMetaData(l(DataChange.remove(SCARY + "2")));
		ms.close();
		ms = msc.init(new DummyProgressMonitor());
		assertTrue(ms.hasMetaData(SCARY));
		assertFalse(ms.hasMetaData(SCARY + "2"));
		assertEquals(SCARY, ms.getMetaData(SCARY));
		ms.changeMetaData(l(DataChange.move(SCARY, "foo")));
		ms.close();
		ms = msc.init(new DummyProgressMonitor());
		assertEquals(SCARY, ms.getMetaData("foo"));
		ms.close();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void metaDataSync() throws FatalStorageException, StoreConfigInvalidException {
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		s1 = sc1.init(new DummyProgressMonitor());
		s1.changeMetaData(l(DataChange.put(SCARY, SCARY), DataChange.put(SCARY + "2", SCARY)));
		s1.changeMetaData(l(DataChange.remove(SCARY + "2")));
		s1.close();
		ms = msc.init(new DummyProgressMonitor());
		assertTrue(ms.hasMetaData(SCARY));
		assertFalse(ms.hasMetaData(SCARY + "2"));
		assertEquals(SCARY, ms.getMetaData(SCARY));
		ms.changeMetaData(l(DataChange.move(SCARY, "foo")));
		ms.close();
		s2 = sc2.init(new DummyProgressMonitor());
		assertEquals(SCARY, s2.getMetaData("foo"));
		s2.close();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void testMultiplexPut() throws FatalStorageException, StoreConfigInvalidException, IOException {
		p = Util.createFile("foobar");
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		ms = msc.init(new DummyProgressMonitor());
		Document d = ms.create();
		int id = d.getID();
		d.change(l(DataChange.put("key", "value"), PageChange.put("page", p)));
		ms.close();
		s1 = sc1.init(new DummyProgressMonitor());
		d = s1.get(id);
		assertEquals("value", d.get("key"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page").getPath())));
		s1.close();
		s2 = sc2.init(new DummyProgressMonitor());
		d = s2.get(id);
		assertEquals("value", d.get("key"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page").getPath())));
		s2.close();
		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void testMultiplexChangeAndRemove() throws FatalStorageException, StoreConfigInvalidException, IOException {
		p = Util.createFile("foobar");
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		ms = msc.init(new DummyProgressMonitor());
		Document d = ms.create();
		int id = d.getID();
		d.change(l(DataChange.put("key", "value"), DataChange.put("key2", "value"), PageChange.put("page", p)));
		d.change(l(DataChange.put("key", "value2"), DataChange.remove("key2"), PageChange.move("page", "page2")));
		ms.close();
		s1 = sc1.init(new DummyProgressMonitor());
		d = s1.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page2").getPath())));
		s1.close();
		s2 = sc2.init(new DummyProgressMonitor());
		d = s2.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page2").getPath())));
		s2.close();
		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}

	@Test
	public void testMultiplexDeleteUndeleteSync() throws FatalStorageException, StoreConfigInvalidException, IOException {
		p = Util.createFile("foobar");
		sf1 = Util.createTempFolder();
		sf2 = Util.createTempFolder();
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf1));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(), typedL(Object.class, sf2));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(), typedL(Object.class, l(sc1, sc2)));
		ms = msc.init(new DummyProgressMonitor());
		Document d = ms.create();
		int id = d.getID();
		d.change(l(DataChange.put("key", "value"), DataChange.put("key2", "value"), PageChange.put("page", p)));
		d.change(l(DataChange.put("key", "value2"), DataChange.remove("key2"), PageChange.move("page", "page2")));
		ms.delete(d);
		ms.close();

		s1 = sc1.init(new DummyProgressMonitor());
		d = s1.getDeleted(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page2").getPath())));
		s1.close();

		s2 = sc2.init(new DummyProgressMonitor());
		d = s2.getDeleted(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page2").getPath())));
		s2.close();

		s1 = sc1.init(new DummyProgressMonitor());
		d = s1.undelete(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page2").getPath())));
		s1.close();

		ms = msc.init(new DummyProgressMonitor());
		d = ms.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page2").getPath())));
		ms.close();

		s2 = sc2.init(new DummyProgressMonitor());
		d = s2.get(id);
		assertEquals("value2", d.get("key"));
		assertFalse(d.has("key2"));
		assertFalse(d.hasPage("page"));
		assertEquals("foobar", Util.getFirstLine(new File(d.getPage("page2").getPath())));
		s2.close();

		p.delete();
		Util.deleteRecursively(sf1);
		Util.deleteRecursively(sf2);
	}
}