package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.TestStoreManagers;
import com.metalbeetle.fruitbat.TestStoreManager;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class DataStorageTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	EnhancedStore s;
	DocIndex i;

	@Test
	public void testCleanStoreValues() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				String rev = s.getRevision();
				String uuid = s.getUUID();
				assertTrue(s.isEmptyStore());
				assertEquals(1, s.getNextRetainedPageNumber());
				m.reboot();
				assertEquals(rev, s.getRevision());
				assertEquals(uuid, s.getUUID());
				assertTrue(s.isEmptyStore());
				assertEquals(1, s.getNextRetainedPageNumber());
				m.reboot();
				assertEquals(rev, s.getRevision());
				assertEquals(uuid, s.getUUID());
				assertTrue(s.isEmptyStore());
				assertEquals(1, s.getNextRetainedPageNumber());
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void metaDataStoreAndRetrieve() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				String rev = s.getRevision();
				s.changeMetaData(l(DataChange.put(SCARY, SCARY), DataChange.put(SCARY + "2", SCARY)));
				s.changeMetaData(l(DataChange.remove(SCARY + "2")));
				assertFalse(s.getRevision().equals(rev));
				assertFalse(s.isEmptyStore());
				m.reboot();
				assertTrue(s.hasMetaData(SCARY));
				assertFalse(s.hasMetaData(SCARY + "2"));
				assertEquals(SCARY, s.getMetaData(SCARY));
				s.changeMetaData(l(DataChange.move(SCARY, "foo")));
				m.reboot();
				assertEquals(SCARY, s.getMetaData("foo"));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void storeAndRetrieve() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				String rev = s.getRevision();
				Document d = s.create();
				assertFalse(s.getRevision().equals(rev));
				d.change(l(DataChange.put(SCARY, SCARY)));
				assertEquals(SCARY, d.get(SCARY));
				d.change(l(DataChange.put(SCARY, SCARY + "2")));
				assertEquals(SCARY + "2", d.get(SCARY));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void delete() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				Document d = s.create();
				d.change(l(DataChange.put(SCARY, SCARY)));
				assertTrue(d.has(SCARY));
				d.change(l(DataChange.remove(SCARY)));
				assertFalse(d.has(SCARY));
				} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void persistence() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				Document d = s.create();
				d.change(l(DataChange.put(SCARY, SCARY)));
				int id = d.getID();
				m.reboot();
				d = s.get(id);
				assertTrue(d.has(SCARY));
				assertEquals(SCARY, d.get(SCARY));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test(expected = FatalStorageException.class)
	public void reallyGone() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				Document d = s.create();
				d.change(l(DataChange.put(SCARY, SCARY)));
				d.change(l(DataChange.remove(SCARY)));
				int id = d.getID();
				m.reboot();
				d = s.get(id);
				d.get(SCARY);
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void multiStore() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				Document d = s.create();
				for (int j = 0; j < 100; j++) {
					d.change(l(DataChange.put("key " + j, "value " + j)));
				}
				int id = d.getID();
				m.reboot();
				d = s.get(id);
				for (int j = 0; j < 100; j++) {
					assertEquals("value " + j, d.get("key " + j));
				}
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void move() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				Document d = s.create();
				d.change(l(DataChange.put(SCARY, SCARY)));
				d.change(l(DataChange.move(SCARY, "foo")));
				assertTrue(d.has("foo"));
				assertFalse(d.has(SCARY));
				int id = d.getID();
				m.reboot();
				d = s.get(id);
				assertTrue(d.has("foo"));
				assertFalse(d.has(SCARY));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void multiChange() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				s = m.getStore();
				i = m.getIndex();
				Document d = s.create();
				d.change(l(
						DataChange.put(SCARY, SCARY),
						DataChange.move(SCARY, "foo"),
						DataChange.put("bar", "quux"),
						DataChange.remove("bar")));
				int id = d.getID();
				m.reboot();
				d = s.get(id);
				assertTrue(d.has("foo"));
				assertEquals(SCARY, d.get("foo"));
				assertFalse(d.has("bar"));
			} finally {
				m.tearDown();
			}
		}
	}
}