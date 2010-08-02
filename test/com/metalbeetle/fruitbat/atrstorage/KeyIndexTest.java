package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.SearchResult;
import com.metalbeetle.fruitbat.util.StringPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class KeyIndexTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	ATRStore s;
	ATRDocIndex index;

	@Test
	public void search() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put(SCARY, SCARY)));

		Document d2 = s.create();
		d2.change(l(DataChange.put(SCARY, SCARY)));
		d2.change(l(DataChange.put("x", SCARY)));

		Document d3 = s.create();
		d3.change(l(DataChange.put(SCARY + "not", SCARY)));

		int id1 = d1.getID();
		int id2 = d2.getID();
		int id3 = d3.getID();

		rebootStore();
		d1 = s.get(id1);
		d2 = s.get(id2);
		d3 = s.get(id3);

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
		Document d1 = s.create();
		d1.change(l(DataChange.put("x", "12345")));
		d1.change(l(DataChange.put("y", "a")));
		Document d2 = s.create();
		d2.change(l(DataChange.put("x", "12345")));
		d2.change(l(DataChange.put("y", "b")));
		Document d3 = s.create();
		d3.change(l(DataChange.put("x", "123456")));
		Document d4 = s.create();
		d4.change(l(DataChange.put("x", "1234")));
		d4.change(l(DataChange.put("y", "a")));
		Document d5 = s.create();
		d5.change(l(DataChange.put("x", "1234000")));
		Document d6 = s.create();
		d6.change(l(DataChange.put("x", "kqopqfekqpo")));
		d6.change(l(DataChange.put("y", "a")));

		int id1 = d1.getID();
		int id2 = d2.getID();
		int id3 = d3.getID();
		int id4 = d4.getID();
		int id5 = d5.getID();
		int id6 = d6.getID();

		rebootStore();
		d1 = s.get(id1);
		d2 = s.get(id2);
		d3 = s.get(id3);
		d4 = s.get(id4);
		d5 = s.get(id5);
		d6 = s.get(id6);

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
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "a")));

		Document d2 = s.create();
		d2.change(l(DataChange.put("a", "a")));
		d2.change(l(DataChange.put("b", "b")));

		Document d3 = s.create();
		d3.change(l(DataChange.put("b", "b")));
		d3.change(l(DataChange.put("c", "c")));

		int id1 = d1.getID();
		int id2 = d2.getID();
		int id3 = d3.getID();

		rebootStore();
		d1 = s.get(id1);
		d2 = s.get(id2);
		d3 = s.get(id3);

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

	void rebootStore() throws FatalStorageException {
		index.close();
		s = new ATRStore(s.getLocation(), new DummyProgressMonitor());
		index = new ATRDocIndex(s, new DummyProgressMonitor(), new StringPool(128));
	}

    @Before
    public void setUp() throws FatalStorageException {
		s = new ATRStore(Util.createTempFolder(), new DummyProgressMonitor());
		index = new ATRDocIndex(s, new DummyProgressMonitor(), new StringPool(128));
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(s.getLocation());
    }
}