package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.TestStoreManager;
import com.metalbeetle.fruitbat.TestStoreManagers;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class NecromancyTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	@Test
	public void modifyWhileDeleted() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				Document d1 = m.getStore().create();
				d1.change(l(DataChange.put("a", "x")));
				int id = d1.getID();
				m.getStore().delete(d1);

				m.reboot();

				d1 = m.getStore().get(id);
				d1.change(l(DataChange.put("a", "y")));

				d1 = m.getStore().undelete(d1);

				assertEquals("y", d1.get("a"));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void getWhileDeleted() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				Document d1 = m.getStore().create();
				d1.change(l(DataChange.put("a", "x")));
				int id = d1.getID();
				m.getStore().delete(d1);
				m.reboot();
				d1 = m.getStore().get(id);
				d1.change(l(DataChange.put("a", "y")));
				assertEquals("y", d1.get("a"));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void noSearchWhileDeleted() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				Document d1 = m.getStore().create();
				d1.change(l(DataChange.put("a", "x")));
				int id = d1.getID();
				m.getStore().delete(d1);
				m.reboot();
				d1 = m.getStore().get(id);
				assertTrue(d1.has(Fruitbat.DEAD_KEY));
				assertFalse(d1.has(Fruitbat.ALIVE_KEY));
				d1.change(l(DataChange.put("a", "y")));

				SearchResult result;
				result = m.getIndex().search(m(p("a", "y"), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
				assertEquals(0, result.docs.size());
				result = m.getIndex().search(m(p("a", "x"), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
				assertEquals(0, result.docs.size());
				result = m.getIndex().search(m(p("a", ""), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
				assertEquals(0, result.docs.size());

				m.getStore().undelete(m.getStore().get(id));

				result = m.getIndex().search(m(p("a", "y"), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
				assertEquals(1, result.docs.size());
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void correctChangeIDAfterCrash() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				Document d1 = m.getStore().create();
				d1.change(l(DataChange.put("a", "x")));
				d1.change(l(DataChange.put("a", "y")));
				int id1 = d1.getID();
				Document d2 = m.getStore().create();
				d2.change(l(DataChange.put("a", "x")));
				d2.change(l(DataChange.put("a", "y")));
				d2.change(l(DataChange.put("a", "z")));
				int id2 = d2.getID();
				String v2 = d2.getRevision();
				m.getStore().delete(d1);
				String v1 = d1.getRevision();
				m.crashAndReboot();
				assertEquals(v1, m.getStore().get(id1).getRevision());
				assertEquals(v2, m.getStore().get(id2).getRevision());
			} finally {
				m.tearDown();
			}
		}
	}
}