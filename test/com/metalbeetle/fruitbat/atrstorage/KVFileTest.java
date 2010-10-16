package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class KVFileTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	@Test
	public void setAndGet() throws IOException, FatalStorageException {
		File f = File.createTempFile("foo", ".atr");
		AppendingKVFile kvf = new AppendingKVFile(f);
		kvf.change(l(DataChange.put(SCARY, SCARY)));
		assertTrue(kvf.has(SCARY));
		assertEquals(SCARY, kvf.get(SCARY));
		kvf = new AppendingKVFile(f);
		assertTrue(kvf.has(SCARY));
		assertEquals(SCARY, kvf.get(SCARY));
		f.delete();
	}

	@Test
	public void remove() throws IOException, FatalStorageException {
		File f = File.createTempFile("foo", ".atr");
		AppendingKVFile kvf = new AppendingKVFile(f);
		kvf.change(l(DataChange.put(SCARY, SCARY)));
		assertTrue(kvf.has(SCARY));
		assertEquals(SCARY, kvf.get(SCARY));
		kvf = new AppendingKVFile(f);
		kvf.change(l(DataChange.remove(SCARY)));
		assertFalse(kvf.has(SCARY));
		kvf = new AppendingKVFile(f);
		kvf.change(l(DataChange.remove(SCARY)));
		assertFalse(kvf.has(SCARY));
		f.delete();
	}

	@Test
	public void move() throws IOException, FatalStorageException {
		File f = File.createTempFile("foo", ".atr");
		AppendingKVFile kvf = new AppendingKVFile(f);
		kvf.change(l(DataChange.put(SCARY, SCARY)));
		assertTrue(kvf.has(SCARY));
		assertEquals(SCARY, kvf.get(SCARY));
		kvf = new AppendingKVFile(f);
		kvf.change(l(DataChange.move(SCARY, "jam")));
		assertFalse(kvf.has(SCARY));
		assertTrue(kvf.has("jam"));
		kvf = new AppendingKVFile(f);
		assertFalse(kvf.has(SCARY));
		assertTrue(kvf.has("jam"));
		assertEquals(SCARY, kvf.get("jam"));
		f.delete();
	}

	@Test
	public void cacheCoherent() throws IOException, FatalStorageException {
		File f = File.createTempFile("foo", ".atr");
		File fCache = File.createTempFile("foo", "-cache.atr");
		AppendingKVFile kvf = new AppendingKVFile(f, fCache, Collections.<String, String>emptyMap());
		kvf.change(l(DataChange.put("foo", "bar")));
		kvf.has("ensure kvf loaded so that saveToCache does something");
		kvf.saveToCache();
		kvf.change(l(DataChange.put("foo", "quux")));
		assertEquals("quux", kvf.get("foo"));
		f.delete();
		fCache.delete();
	}
}