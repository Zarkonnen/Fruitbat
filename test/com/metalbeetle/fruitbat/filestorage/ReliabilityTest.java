package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class ReliabilityTest {
	public static final String SCARY = "\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	@Test
	public void testDataThereAfterCrash() throws FatalStorageException {
		File loc = Util.createTempFolder();
		try {
			IntentionallyCrashingFileStreamFactory crashy = new IntentionallyCrashingFileStreamFactory();
			FileStore fs = new FileStore(loc, new DummyProgressMonitor(), crashy);
			Document d = fs.create();
			int id = d.getID();
			d.change(l(DataChange.put("jam", "jam"), DataChange.put(SCARY, SCARY)));
			try {
				crashy.crashAtOutput((byte) 'a');
				d.change(l(DataChange.put("notthere", "foo"), DataChange.put("a", "b")));
				assertTrue("Should have crashed!", false);
			} catch (Exception e) {
				fs = new FileStore(loc, new DummyProgressMonitor(), crashy);
				d = fs.get(id);
				assertTrue(d.has(SCARY));
				assertFalse(d.has("notthere"));
				assertFalse(d.has("a"));
				assertEquals(1, fs.getIndex().search(m(p("jam", "")), 1).docs.size());
				assertEquals(0, fs.getIndex().search(m(p("notthere", "")), 0).docs.size());
			}
		} finally {
			Util.deleteRecursively(loc);
		}
	}
}
