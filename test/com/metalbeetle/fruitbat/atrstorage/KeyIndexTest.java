package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.SearchResult;
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
	public void search() {
		Document d1 = s.create();
		d1.put(SCARY, SCARY);

		Document d2 = s.create();
		d2.put(SCARY, SCARY);
		d2.put("x", SCARY);

		Document d3 = s.create();
		d3.put(SCARY + "not", SCARY);

		String id1 = d1.getID();
		String id2 = d2.getID();
		String id3 = d3.getID();

		rebootStore();
		d1 = s.get(id1);
		d2 = s.get(id2);
		d3 = s.get(id3);

		// Searching for SCARY should yield d1 and d2.
		SearchResult result;
		result = index.search(m(p(SCARY, "")), DocIndex.ALL_DOCS, DocIndex.NO_TIMEOUT);
		assertTrue(result.docs.contains(d1));
		assertTrue(result.docs.contains(d2));
		assertFalse(result.docs.contains(d3));

		// Searching for SCARY and x should just yield d2.
		result = index.search(m(p(SCARY, ""), p("x", SCARY)), DocIndex.ALL_DOCS, DocIndex.NO_TIMEOUT);
		assertFalse(result.docs.contains(d1));
		assertTrue(result.docs.contains(d2));
		assertFalse(result.docs.contains(d3));
	}

	@Test
	public void coTags() {
		Document d1 = s.create();
		d1.put("a", "a");

		Document d2 = s.create();
		d2.put("a", "a");
		d2.put("b", "b");

		Document d3 = s.create();
		d3.put("b", "b");
		d3.put("c", "c");

		String id1 = d1.getID();
		String id2 = d2.getID();
		String id3 = d3.getID();

		rebootStore();
		d1 = s.get(id1);
		d2 = s.get(id2);
		d3 = s.get(id3);

		SearchResult result;

		// Check that searching for a gives a narrowing suggestion of b only.
		result = index.search(m(p("a", "")), DocIndex.ALL_DOCS, DocIndex.NO_TIMEOUT);
		assertFalse(result.narrowingTags.contains("a"));
		assertTrue(result.narrowingTags.contains("b"));
		assertFalse(result.narrowingTags.contains("c"));

		// Check that searching for b gives a narrowing suggestion of a and c.
		result = index.search(m(p("b", "")), DocIndex.ALL_DOCS, DocIndex.NO_TIMEOUT);
		assertTrue(result.narrowingTags.contains("a"));
		assertFalse(result.narrowingTags.contains("b"));
		assertTrue(result.narrowingTags.contains("c"));
	}

	void rebootStore() {
		index.close();
		s = new ATRStore(s.getLocation());
		index = new ATRDocIndex(s);
	}

    @Before
    public void setUp() {
		s = new ATRStore(Util.createTempFolder());
		index = new ATRDocIndex(s);
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(s.getLocation());
    }
}