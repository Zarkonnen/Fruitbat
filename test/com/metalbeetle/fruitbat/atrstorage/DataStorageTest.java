package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.storage.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DataStorageTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	ATRStore s;

	@Test
	public void storeAndRetrieve() {
		Document d = s.create();
		d.put(SCARY, SCARY);
		assertEquals(SCARY, d.get(SCARY));
		d.put(SCARY, SCARY + "2");
		assertEquals(SCARY + "2", d.get(SCARY));
	}

	@Test
	public void delete() {
		Document d = s.create();
		assertEquals(0, d.keys().size());
		d.put(SCARY, SCARY);
		assertTrue(d.has(SCARY));
		d.remove(SCARY);
		assertFalse(d.has(SCARY));
	}

	@Test
	public void persistence() {
		Document d = s.create();
		d.put(SCARY, SCARY);
		String id = d.getID();
		rebootStore();
		d = s.get(id);
		assertTrue(d.has(SCARY));
		assertEquals(SCARY, d.get(SCARY));
	}

	@Test(expected = RuntimeException.class)
	public void reallyGone() {
		Document d = s.create();
		d.put(SCARY, SCARY);
		d.remove(SCARY);
		String id = d.getID();
		rebootStore();
		d = s.get(id);
		d.get(SCARY);
	}

	@Test
	public void multiStore() {
		Document d = s.create();
		for (int i = 0; i < 100; i++) {
			d.put("key " + i, "value " + i);
		}
		String id = d.getID();
		rebootStore();
		d = s.get(id);
		for (int i = 0; i < 100; i++) {
			assertEquals("value " + i, d.get("key " + i));
		}
	}

	@Test
	public void move() {
		Document d = s.create();
		d.put(SCARY, SCARY);
		d.move(SCARY, "foo");
		assertTrue(d.has("foo"));
		assertFalse(d.has(SCARY));
		rebootStore();
		assertTrue(d.has("foo"));
		assertFalse(d.has(SCARY));
	}

	void rebootStore() {
		s = new ATRStore(s.getLocation());
	}

    @Before
    public void setUp() {
		s = new ATRStore(Util.createTempFolder());
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(s.getLocation());
    }
}