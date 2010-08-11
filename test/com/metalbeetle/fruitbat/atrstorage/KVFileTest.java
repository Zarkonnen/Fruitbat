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

	@Test
	public void cacheCoherent() throws IOException, FatalStorageException {
		File f = File.createTempFile("foo", ".atr");
		File fCache = File.createTempFile("foo", "-cache.atr");
		KVFile kvf = new KVFile(f, fCache, Collections.<String, String>emptyMap());
		kvf.change(l(DataChange.put("foo", "bar")));
		kvf.has("ensure kvf loaded so that saveToCache does something");
		kvf.saveToCache();
		kvf.change(l(DataChange.put("foo", "quux")));
		assertEquals("quux", kvf.get("foo"));
	}
}