package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import com.metalbeetle.fruitbat.KVFileManagers;
import com.metalbeetle.fruitbat.KVFileManager;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class KVFileTest {
	public static final String SCARY = "\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	@Test
	public void setAndGet() throws Exception {
		for (KVFileManager m : KVFileManagers.get()) {
			try {
				m.setUp(false);
				KVFile kvf = m.get();
				kvf.change(l(DataChange.put(SCARY, SCARY)));
				assertTrue(kvf.has(SCARY));
				assertEquals(SCARY, kvf.get(SCARY));
				m.reboot();
				kvf = m.get();
				assertTrue(kvf.has(SCARY));
				assertEquals(SCARY, kvf.get(SCARY));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void remove() throws Exception {
		for (KVFileManager m : KVFileManagers.get()) {
			try {
				m.setUp(false);
				KVFile kvf = m.get();
				kvf.change(l(DataChange.put(SCARY, SCARY)));
				assertTrue(kvf.has(SCARY));
				assertEquals(SCARY, kvf.get(SCARY));
				m.reboot();
				kvf = m.get();
				kvf.change(l(DataChange.remove(SCARY)));
				assertFalse(kvf.has(SCARY));
				m.reboot();
				kvf = m.get();
				kvf.change(l(DataChange.remove(SCARY)));
				assertFalse(kvf.has(SCARY));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void move() throws Exception {
		for (KVFileManager m : KVFileManagers.get()) {
			try {
				m.setUp(false);
				KVFile kvf = m.get();
				kvf.change(l(DataChange.put(SCARY, SCARY)));
				assertTrue(kvf.has(SCARY));
				assertEquals(SCARY, kvf.get(SCARY));
				m.reboot();
				kvf = m.get();
				kvf.change(l(DataChange.move(SCARY, "jam")));
				assertFalse(kvf.has(SCARY));
				assertTrue(kvf.has("jam"));
				m.reboot();
				kvf = m.get();
				assertFalse(kvf.has(SCARY));
				assertTrue(kvf.has("jam"));
				assertEquals(SCARY, kvf.get("jam"));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void cacheCoherent() throws Exception {
		for (KVFileManager m : KVFileManagers.get()) {
			try {
				m.setUp(true);
				KVFile kvf = m.get();
				kvf.change(l(DataChange.put("foo", "bar")));
				kvf.has("ensure kvf loaded so that saveToCache does something");
				kvf.saveToCache();
				kvf.change(l(DataChange.put("foo", "quux")));
				assertEquals("quux", kvf.get("foo"));
				m.reboot();
				kvf = m.get();
				assertEquals("quux", kvf.get("foo"));
			} finally {
				m.tearDown();
			}
		}
	}
}