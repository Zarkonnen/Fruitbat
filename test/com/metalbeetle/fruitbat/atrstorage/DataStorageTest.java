package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class DataStorageTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	ATRStore s;
	ATRDocIndex i;

	@Test
	public void testCleanStoreValues() throws FatalStorageException, InterruptedException {
		String rev = s.getRevision();
		String uuid = s.getUUID();
		assertTrue(s.isEmptyStore());
		assertEquals(1, s.getNextRetainedPageNumber());
		rebootStore();
		assertEquals(rev, s.getRevision());
		assertEquals(uuid, s.getUUID());
		assertTrue(s.isEmptyStore());
		assertEquals(1, s.getNextRetainedPageNumber());
		rebootStore();
		assertEquals(rev, s.getRevision());
		assertEquals(uuid, s.getUUID());
		assertTrue(s.isEmptyStore());
		assertEquals(1, s.getNextRetainedPageNumber());
	}

	@Test
	public void metaDataStoreAndRetrieve() throws FatalStorageException {
		String rev = s.getRevision();
		s.changeMetaData(l(DataChange.put(SCARY, SCARY), DataChange.put(SCARY + "2", SCARY)));
		s.changeMetaData(l(DataChange.remove(SCARY + "2")));
		assertFalse(s.getRevision().equals(rev));
		assertFalse(s.isEmptyStore());
		rebootStore();
		assertTrue(s.hasMetaData(SCARY));
		assertFalse(s.hasMetaData(SCARY + "2"));
		assertEquals(SCARY, s.getMetaData(SCARY));
		s.changeMetaData(l(DataChange.move(SCARY, "foo")));
		rebootStore();
		assertEquals(SCARY, s.getMetaData("foo"));
	}

	@Test
	public void storeAndRetrieve() throws FatalStorageException {
		String rev = s.getRevision();
		Document d = s.create();
		assertFalse(s.getRevision().equals(rev));
		d.change(l(DataChange.put(SCARY, SCARY)));
		assertEquals(SCARY, d.get(SCARY));
		d.change(l(DataChange.put(SCARY, SCARY + "2")));
		assertEquals(SCARY + "2", d.get(SCARY));
	}

	@Test
	public void delete() throws FatalStorageException {
		Document d = s.create();
		d.change(l(DataChange.put(SCARY, SCARY)));
		assertTrue(d.has(SCARY));
		d.change(l(DataChange.remove(SCARY)));
		assertFalse(d.has(SCARY));
	}

	@Test
	public void persistence() throws FatalStorageException {
		Document d = s.create();
		d.change(l(DataChange.put(SCARY, SCARY)));
		int id = d.getID();
		rebootStore();
		d = s.get(id);
		assertTrue(d.has(SCARY));
		assertEquals(SCARY, d.get(SCARY));
	}

	@Test(expected = FatalStorageException.class)
	public void reallyGone() throws FatalStorageException {
		Document d = s.create();
		d.change(l(DataChange.put(SCARY, SCARY)));
		d.change(l(DataChange.remove(SCARY)));
		int id = d.getID();
		rebootStore();
		d = s.get(id);
		d.get(SCARY);
	}

	@Test
	public void multiStore() throws FatalStorageException {
		Document d = s.create();
		for (int j = 0; j < 100; j++) {
			d.change(l(DataChange.put("key " + j, "value " + j)));
		}
		int id = d.getID();
		rebootStore();
		d = s.get(id);
		for (int j = 0; j < 100; j++) {
			assertEquals("value " + j, d.get("key " + j));
		}
	}

	@Test
	public void move() throws FatalStorageException {
		Document d = s.create();
		d.change(l(DataChange.put(SCARY, SCARY)));
		d.change(l(DataChange.move(SCARY, "foo")));
		assertTrue(d.has("foo"));
		assertFalse(d.has(SCARY));
		int id = d.getID();
		rebootStore();
		d = s.get(id);
		assertTrue(d.has("foo"));
		assertFalse(d.has(SCARY));
	}

	@Test
	public void multiChange() throws FatalStorageException {
		Document d = s.create();
		d.change(l(
				DataChange.put(SCARY, SCARY),
				DataChange.move(SCARY, "foo"),
				DataChange.put("bar", "quux"),
				DataChange.remove("bar")));
		int id = d.getID();
		rebootStore();
		d = s.get(id);
		assertTrue(d.has("foo"));
		assertEquals(SCARY, d.get("foo"));
		assertFalse(d.has("bar"));
	}

	void rebootStore() throws FatalStorageException {
		s.close();
		s = new ATRStore(s.getLocation(), new DummyProgressMonitor());
		i = s.index;
	}

    @Before
    public void setUp() throws FatalStorageException {
		s = new ATRStore(Util.createTempFolder(), new DummyProgressMonitor());
		i = s.index;
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(s.getLocation());
    }
}