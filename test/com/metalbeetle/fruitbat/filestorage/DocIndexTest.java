package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.hierarchicalstorage.HSIndex;
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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class DocIndexTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	EnhancedStore s;
	FileStore fileS;
	HSIndex index;

	@Test
	public void search() throws FatalStorageException {
		Document d1 = fileS.create();
		d1.change(l(DataChange.put(SCARY, SCARY)));

		Document d2 = fileS.create();
		d2.change(l(DataChange.put(SCARY, SCARY)));
		d2.change(l(DataChange.put("x", SCARY)));

		Document d3 = fileS.create();
		d3.change(l(DataChange.put(SCARY + "not", SCARY)));

		int id1 = d1.getID();
		int id2 = d2.getID();
		int id3 = d3.getID();

		rebootStore();
		d1 = fileS.get(id1);
		d2 = fileS.get(id2);
		d3 = fileS.get(id3);

		// Searching for SCARY should yield d1 and d2.
		SearchResult result;
		result = index.search(m(p(SCARY, "")), DocIndex.ALL_DOCS);
		assertTrue(result.docs.contains(d1));
		assertTrue(result.docs.contains(d2));
		assertFalse(result.docs.contains(d3));

		// Searching for SCARY and x should just yield d2.
		result = index.search(m(p(SCARY, ""), p("x", SCARY)), DocIndex.ALL_DOCS);
		assertFalse(result.docs.contains(d1));
		assertTrue(result.docs.contains(d2));
		assertFalse(result.docs.contains(d3));
	}

	@Test
	public void searchValuePrefixes() throws FatalStorageException {
		Document d1 = fileS.create();
		d1.change(l(DataChange.put("x", "12345")));
		d1.change(l(DataChange.put("y", "a")));
		Document d2 = fileS.create();
		d2.change(l(DataChange.put("x", "12345")));
		d2.change(l(DataChange.put("y", "b")));
		Document d3 = fileS.create();
		d3.change(l(DataChange.put("x", "123456")));
		Document d4 = fileS.create();
		d4.change(l(DataChange.put("x", "1234")));
		d4.change(l(DataChange.put("y", "a")));
		Document d5 = fileS.create();
		d5.change(l(DataChange.put("x", "1234000")));
		Document d6 = fileS.create();
		d6.change(l(DataChange.put("x", "kqopqfekqpo")));
		d6.change(l(DataChange.put("y", "a")));

		int id1 = d1.getID();
		int id2 = d2.getID();
		int id3 = d3.getID();
		int id4 = d4.getID();
		int id5 = d5.getID();
		int id6 = d6.getID();

		rebootStore();
		d1 = fileS.get(id1);
		d2 = fileS.get(id2);
		d3 = fileS.get(id3);
		d4 = fileS.get(id4);
		d5 = fileS.get(id5);
		d6 = fileS.get(id6);

		SearchResult result;
		result = index.search(m(p("x", "12345")), DocIndex.ALL_DOCS);
		assertTrue(result.docs.contains(d1));
		assertTrue(result.docs.contains(d2));
		assertTrue(result.docs.contains(d3));
		assertFalse(result.docs.contains(d4));
		assertFalse(result.docs.contains(d5));
		assertFalse(result.docs.contains(d6));

		result = index.search(m(p("x", "123456")), DocIndex.ALL_DOCS);
		assertFalse(result.docs.contains(d1));
		assertFalse(result.docs.contains(d2));
		assertTrue(result.docs.contains(d3));
		assertFalse(result.docs.contains(d4));
		assertFalse(result.docs.contains(d5));
		assertFalse(result.docs.contains(d6));

		result = index.search(m(p("x", "1234")), DocIndex.ALL_DOCS);
		assertTrue(result.docs.contains(d1));
		assertTrue(result.docs.contains(d2));
		assertTrue(result.docs.contains(d3));
		assertTrue(result.docs.contains(d4));
		assertTrue(result.docs.contains(d5));
		assertFalse(result.docs.contains(d6));

		result = index.search(m(p("x", "")), DocIndex.ALL_DOCS);
		assertTrue(result.docs.contains(d1));
		assertTrue(result.docs.contains(d2));
		assertTrue(result.docs.contains(d3));
		assertTrue(result.docs.contains(d4));
		assertTrue(result.docs.contains(d5));
		assertTrue(result.docs.contains(d6));

		result = index.search(m(p("x", "12345"), p("y", "a")), DocIndex.ALL_DOCS);
		assertTrue(result.docs.contains(d1));
		assertFalse(result.docs.contains(d2));
		assertFalse(result.docs.contains(d3));
		assertFalse(result.docs.contains(d4));
		assertFalse(result.docs.contains(d5));
		assertFalse(result.docs.contains(d6));

		result = index.search(m(p("x", "12"), p("y", "a")), DocIndex.ALL_DOCS);
		assertTrue(result.docs.contains(d1));
		assertFalse(result.docs.contains(d2));
		assertFalse(result.docs.contains(d3));
		assertTrue(result.docs.contains(d4));
		assertFalse(result.docs.contains(d5));
		assertFalse(result.docs.contains(d6));
	}

	@Test
	public void coTags() throws FatalStorageException {
		Document d1 = fileS.create();
		d1.change(l(DataChange.put("a", "a")));

		Document d2 = fileS.create();
		d2.change(l(DataChange.put("a", "a")));
		d2.change(l(DataChange.put("b", "b")));

		Document d3 = fileS.create();
		d3.change(l(DataChange.put("b", "b")));
		d3.change(l(DataChange.put("c", "c")));

		int id1 = d1.getID();
		int id2 = d2.getID();
		int id3 = d3.getID();

		rebootStore();
		d1 = fileS.get(id1);
		d2 = fileS.get(id2);
		d3 = fileS.get(id3);

		SearchResult result;

		// Check that searching for a gives a narrowing suggestion of b only.
		result = index.search(m(p("a", "")), DocIndex.ALL_DOCS);
		assertFalse(result.narrowingTags.contains("a"));
		assertTrue(result.narrowingTags.contains("b"));
		assertFalse(result.narrowingTags.contains("c"));

		// Check that searching for b gives a narrowing suggestion of a and c.
		result = index.search(m(p("b", "")), DocIndex.ALL_DOCS);
		assertTrue(result.narrowingTags.contains("a"));
		assertFalse(result.narrowingTags.contains("b"));
		assertTrue(result.narrowingTags.contains("c"));
	}

	@Test
	public void postDelete() throws FatalStorageException {
		Document d1 = fileS.create();
		d1.change(l(DataChange.put(SCARY, SCARY)));
		assertTrue(d1.has(Fruitbat.ALIVE_KEY));

		SearchResult result;
		result = index.search(m(p(SCARY, ""), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(1, result.docs.size());

		s.delete(d1);
		assertFalse(d1.has(Fruitbat.ALIVE_KEY));
		assertTrue(d1.has(Fruitbat.DEAD_KEY));

		result = index.search(m(p(SCARY, ""), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());

		rebootStore();

		result = index.search(m(p(SCARY, ""), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());
	}

	@Test
	public void postUnDelete() throws FatalStorageException {
		Document d1 = fileS.create();
		int id1 = d1.getID();
		d1.change(l(DataChange.put(SCARY, SCARY)));
		s.delete(d1);

		rebootStore();

		s.undelete(s.get(id1));

		SearchResult result;
		result = index.search(m(p(SCARY, ""), p(Fruitbat.ALIVE_KEY, "")), DocIndex.ALL_DOCS);
		assertEquals(1, result.docs.size());
	}

	void rebootStore() throws FatalStorageException {
		index.close();
		fileS = new FileStore(fileS.f, new DummyProgressMonitor());
		s = new EnhancedStore(fileS);
		index = (HSIndex) fileS.getIndex();
	}

    @Before
    public void setUp() throws FatalStorageException {
		fileS = new FileStore(Util.createTempFolder(), new DummyProgressMonitor());
		s = new EnhancedStore(fileS);
		index = (HSIndex) fileS.getIndex();
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(fileS.f);
    }
}