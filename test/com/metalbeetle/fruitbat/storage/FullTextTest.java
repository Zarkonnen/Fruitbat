package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.TestStoreManager;
import com.metalbeetle.fruitbat.TestStoreManagers;
import com.metalbeetle.fruitbat.io.FileSrc;
import java.io.File;
import java.io.FileWriter;
import org.junit.Test;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static org.junit.Assert.*;

/** Tests involving indexes. */
public class FullTextTest {
	@Test
	public void fullTextSearch() throws Exception {
		File f = mkTmpFile("Mene Mene Tekel Upharsin");
		File f2 = mkTmpFile("Mene Mene Jamcat Upharsin");
		File f3 = mkTmpFile("Mene Catjam Tekel Upharsin");
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				if (m.getStore().getFullTextIndex() == null) { continue; }
				Document d = m.getStore().create();
				int id = d.getID();
				d.change(l(PageChange.put(Fruitbat.FULLTEXT_PREFIX + "jam", new FileSrc(f))));
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene", "Upharsin")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("jam")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel"), l("Upharsin")),
					l(d)).size());
				m.reboot();
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene", "Upharsin")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("jam")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel"), l("Upharsin")),
					l(d)).size());

				d = m.getStore().get(id);
				d.change(l(PageChange.move(Fruitbat.FULLTEXT_PREFIX + "jam", Fruitbat.FULLTEXT_PREFIX + "cat")));
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene", "Upharsin")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("jam")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel"), l("Upharsin")),
					l(d)).size());
				m.reboot();
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene", "Upharsin")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("jam")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel"), l("Upharsin")),
					l(d)).size());

				Document d2 = m.getStore().create();
				int id2 = d2.getID();
				d2.change(l(PageChange.put(Fruitbat.FULLTEXT_PREFIX + "jamcat", new FileSrc(f2))));
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d, d2)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel")),
					l(d, d2)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Jamcat")),
					l(d, d2)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Tekel"), l("Jamcat")),
					l(d, d2)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Jamcat")),
					l(d, d2)).size());
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Upharsin")),
					l(d, d2)).size());
				m.reboot();
				d = m.getStore().get(id);
				d2 = m.getStore().get(id2);
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d, d2)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel")),
					l(d, d2)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Jamcat")),
					l(d, d2)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Tekel"), l("Jamcat")),
					l(d, d2)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Jamcat")),
					l(d, d2)).size());
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Upharsin")),
					l(d, d2)).size());

				Document d3 = m.getStore().create();
				int id3 = d3.getID();
				d3.change(l(PageChange.put(Fruitbat.FULLTEXT_PREFIX + "catjam", new FileSrc(f3))));
				assertEquals(3, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d, d2, d3)).size());
				assertEquals(3, m.getStore().getFullTextIndex().query(l(l("Mene"), l("Upharsin")),
					l(d, d2, d3)).size());
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Upharsin")),
					l(d, d2, d3)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Catjam")),
					l(d, d2, d3)).size());
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene"), l("Tekel")),
					l(d, d2, d3)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel"), l("Tekel")),
					l(d, d2, d3)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Catjam"), l("Jamcat")),
					l(d, d2, d3)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Fruitcat"), l("Tekel")),
					l(d, d2, d3)).size());
				m.reboot();
				d = m.getStore().get(id);
				d2 = m.getStore().get(id2);
				d3 = m.getStore().get(id3);
				assertEquals(3, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d, d2, d3)).size());
				assertEquals(3, m.getStore().getFullTextIndex().query(l(l("Mene"), l("Upharsin")),
					l(d, d2, d3)).size());
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Upharsin")),
					l(d, d2, d3)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene", "Mene"), l("Catjam")),
					l(d, d2, d3)).size());
				assertEquals(2, m.getStore().getFullTextIndex().query(l(l("Mene"), l("Tekel")),
					l(d, d2, d3)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene", "Tekel"), l("Tekel")),
					l(d, d2, d3)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Catjam"), l("Jamcat")),
					l(d, d2, d3)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Fruitcat"), l("Tekel")),
					l(d, d2, d3)).size());
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void moveAndDeleteFullText() throws Exception {
		File f = mkTmpFile("Mene Mene Tekel Upharsin");
		File f2 = mkTmpFile("Mene Mene Jamcat Upharsin");
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				if (m.getStore().getFullTextIndex() == null) { continue; }
				Document d = m.getStore().create();
				int id = d.getID();
				d.change(l(PageChange.put(Fruitbat.FULLTEXT_PREFIX + "a", new FileSrc(f))));
				d.change(l(PageChange.put(Fruitbat.FULLTEXT_PREFIX + "b", new FileSrc(f2))));
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Tekel")),
					l(d)).size());
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Jamcat")),
					l(d)).size());
				d.change(l(PageChange.remove(Fruitbat.FULLTEXT_PREFIX + "a")));
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Tekel")),
					l(d)).size());
				d.change(l(PageChange.remove(Fruitbat.FULLTEXT_PREFIX + "b")));
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				d.change(l(PageChange.put("a", new FileSrc(f))));
				assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
				d.change(l(PageChange.move("a", Fruitbat.FULLTEXT_PREFIX + "a")));
				assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Mene")),
					l(d)).size());
			} finally {
				m.tearDown();
			}
		}
	}

	File mkTmpFile(String contents) throws Exception {
		File f = File.createTempFile("foo", ".txt");
		f.deleteOnExit();
		FileWriter fw = null;
		try {
			fw = new FileWriter(f);
			fw.write(contents);
		} finally {
			fw.flush();
			fw.close();
		}
		return f;
	}
}
