package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.multiplexstorage.MultiplexStore;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.SearchResult;
import java.io.File;
import java.util.List;
import java.util.Map;
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

	@Test
	public void testSwitchoverDuringSearch() throws FatalStorageException {
		File loc1 = Util.createTempFolder();
		File loc2 = Util.createTempFolder();
		try {
			FileStore backupS = new FileStore(loc1, new DummyProgressMonitor()) {
				@Override
				/** Pretend to be fast so we get to be the store that gets read from. */
				public int getLag() { return 100; }
			};
			FileStore mainS = new FileStore(loc2, new DummyProgressMonitor());
			MultiplexStore ms = new MultiplexStore(l(
					new EnhancedStore(mainS),
					new EnhancedStore(backupS)
			), new DummyProgressMonitor());
			Document d = ms.create();
			d.change(l(DataChange.put("jam", "jam")));
			ms.close();

			backupS = new FileStore(loc1, new DummyProgressMonitor()) {
				@Override
				/** Pretend to be fast so we get to be the store that gets read from. */
				public int getLag() { return 100; }
			};
			mainS = new FileStore(loc2, new DummyProgressMonitor());
			ms = new MultiplexStore(l(
					new EnhancedStore(mainS),
					new EnhancedStore(backupS) {
						@Override
						public DocIndex getIndex() {
							final DocIndex di = super.getIndex();
							return new DocIndex() {
								public SearchResult search(Map<String, String> searchKV, int maxDocs) throws FatalStorageException {
									throw new FatalStorageException("Planned exception.");
								}
								public boolean isKey(String key) throws FatalStorageException { return di.isKey(key); }
								public List<String> allKeys() throws FatalStorageException { return di.allKeys(); }
								public void close() { di.close(); }
							};
						}
					}
			), new DummyProgressMonitor());

			assertEquals(2, ms.enabledStores());
			assertEquals(1, ms.getIndex().search(m(p("jam", "")), 1).docs.size());
			assertEquals(1, ms.enabledStores());
		} finally {
			Util.deleteRecursively(loc1);
			Util.deleteRecursively(loc2);
		}
	}

	@Test
	public void testSaveCrashResiliency() throws FatalStorageException {
		File loc1 = Util.createTempFolder();
		File loc2 = Util.createTempFolder();
		try {
			IntentionallyCrashingFileStreamFactory crashy = new IntentionallyCrashingFileStreamFactory();
			FileStore backupS = new FileStore(loc1, new DummyProgressMonitor(), crashy) {
				@Override
				/** Pretend to be fast so we get to be the store that gets read from. */
				public int getLag() { return 100; }
			};
			FileStore mainS = new FileStore(loc2, new DummyProgressMonitor());
			MultiplexStore ms = new MultiplexStore(l(
					new EnhancedStore(mainS),
					new EnhancedStore(backupS)
			), new DummyProgressMonitor());
			Document d = ms.create();
			int id = d.getID();
			d.change(l(DataChange.put("jam", "jam")));
			crashy.crashAtOutput((byte) ':');
			d.change(l(DataChange.put("cat", "cat")));
			assertEquals(1, ms.enabledStores());
			assertEquals(1, ms.getIndex().search(m(p("cat", "")), 1).docs.size());
			ms.close();

			backupS = new FileStore(loc1, new DummyProgressMonitor()) {
				@Override
				/** Pretend to be fast so we get to be the store that gets read from. */
				public int getLag() { return 100; }
			};
			mainS = new FileStore(loc2, new DummyProgressMonitor());
			ms = new MultiplexStore(l(
					new EnhancedStore(mainS),
					new EnhancedStore(backupS)
			), new DummyProgressMonitor());

			assertEquals(2, ms.enabledStores());
			assertEquals(1, ms.getIndex().search(m(p("jam", "")), 1).docs.size());
			assertEquals(1, ms.getIndex().search(m(p("cat", "")), 1).docs.size());
			assertEquals(2, ms.enabledStores());

			d = ms.get(id);
			assertTrue(d.has("jam"));
			assertTrue(d.has("cat"));
		} finally {
			Util.deleteRecursively(loc1);
			Util.deleteRecursively(loc2);
		}
	}
}
