package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.SearchResult;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

/**
 *
 * @author zar
 */
public class NecromancyTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	EnhancedStore s;
	DocIndex i;

	@Test
	public void modifyWhileDeleted() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		int id = d1.getID();
		s.delete(d1);

		rebootStore();

		d1 = s.get(id);
		d1.change(l(DataChange.put("a", "y")));

		d1 = s.undelete(d1);

		assertEquals("y", d1.get("a"));
	}

	@Test
	public void getWhileDeleted() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		int id = d1.getID();
		s.delete(d1);
		rebootStore();
		d1 = s.get(id);
		d1.change(l(DataChange.put("a", "y")));
		assertEquals("y", d1.get("a"));
	}

	@Test
	public void noSearchWhileDeleted() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		int id = d1.getID();
		s.delete(d1);
		rebootStore();
		d1 = s.get(id);
		assertTrue(d1.has(Fruitbat.DEAD_KEY));
		assertFalse(d1.has(Fruitbat.ALIVE_KEY));
		d1.change(l(DataChange.put("a", "y")));

		SearchResult result;
		result = i.search(m(p("a", "y"), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());
		result = i.search(m(p("a", "x"), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());
		result = i.search(m(p("a", ""), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());

		s.undelete(s.get(id));

		result = i.search(m(p("a", "y"), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(1, result.docs.size());
	}

	@Test
	public void correctChangeIDAfterCrash() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		d1.change(l(DataChange.put("a", "y")));
		int id1 = d1.getID();
		Document d2 = s.create();
		d2.change(l(DataChange.put("a", "x")));
		d2.change(l(DataChange.put("a", "y")));
		d2.change(l(DataChange.put("a", "z")));
		int id2 = d2.getID();
		String v2 = d2.getRevision();
		s.delete(d1);
		String v1 = d1.getRevision();
		crashAndRebootStore();
		assertEquals(v1, s.get(id1).getRevision());
		assertEquals(v2, s.get(id2).getRevision());
	}

	void rebootStore() throws FatalStorageException {
		s.close();
		s = new EnhancedStore(new FileStore(((FileStore) s.s).f, new DummyProgressMonitor()));
		i = s.getIndex();
	}

	void crashAndRebootStore() throws FatalStorageException {
		s = new EnhancedStore(new FileStore(((FileStore) s.s).f, new DummyProgressMonitor()));
		i = s.getIndex();
	}

    @Before
    public void setUp() throws FatalStorageException {
		s = new EnhancedStore(new FileStore(Util.createTempFolder(), new DummyProgressMonitor()));
		i = s.getIndex();
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(((FileStore) s.s).f);
    }
}