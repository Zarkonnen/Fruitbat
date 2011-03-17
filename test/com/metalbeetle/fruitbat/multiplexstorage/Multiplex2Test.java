package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.TestStoreManagers.MultiplexSM;
import com.metalbeetle.fruitbat.io.FileSrc;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.PageChange;
import java.io.File;
import java.io.FileWriter;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class Multiplex2Test {
	@Test
	public void dataConsistentAfterChangingDocumentReturnedFromSearch() throws Exception {
		MultiplexSM m = new MultiplexSM();
		try {
			m.setUp();
			Document d = m.getStore().create();
			int id = d.getID();
			d.change(l(DataChange.put("x", "y")));
			Document d2 = m.getIndex().search(m(p("x", "y")), 1).docs.get(0);
			d2.change(l(DataChange.put("x", "z")));
			m.reboot();
			d = m.getStore().get(id);
			assertEquals("z", d.get("x"));
			assertEquals("z", ((MultiplexStore) m.getStore().s).stores.get(0).get(id).get("x"));
			assertEquals("z", ((MultiplexStore) m.getStore().s).stores.get(1).get(id).get("x"));
		} finally {
			m.tearDown();
		}
	}

	@Test
	public void syncFulltext() throws Exception {
		MultiplexSM m = new MultiplexSM();
		try {
			m.setUp();
			EnhancedStore master = ((MultiplexStore) m.getStore().s).stores.get(0);
			Document d = master.create();
			int id = d.getID();
			File f = mkTmpFile("Thessaloniki");

			// Create
			d.change(l(PageChange.put(Fruitbat.FULLTEXT_PREFIX + "x", new FileSrc(f))));
			m.reboot();
			d = m.getStore().get(id);
			assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Thessaloniki")),
					l(d)).size());

			// Move
			master = ((MultiplexStore) m.getStore().s).stores.get(0);
			d = master.get(id);
			d.change(l(PageChange.move(Fruitbat.FULLTEXT_PREFIX + "x",
					Fruitbat.FULLTEXT_PREFIX + "y")));
			m.reboot();
			d = m.getStore().get(id);
			assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Thessaloniki")),
					l(d)).size());

			// Move into non-fulltext-prefix
			master = ((MultiplexStore) m.getStore().s).stores.get(0);
			d = master.get(id);
			d.change(l(PageChange.move(Fruitbat.FULLTEXT_PREFIX + "y", "z")));
			m.reboot();
			d = m.getStore().get(id);
			assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Thessaloniki")),
					l(d)).size());

			// Move back into fulltext-prefix
			master = ((MultiplexStore) m.getStore().s).stores.get(0);
			d = master.get(id);
			d.change(l(PageChange.move("z", Fruitbat.FULLTEXT_PREFIX + "a")));
			m.reboot();
			d = m.getStore().get(id);
			assertEquals(1, m.getStore().getFullTextIndex().query(l(l("Thessaloniki")),
					l(d)).size());

			// Delete
			master = ((MultiplexStore) m.getStore().s).stores.get(0);
			d = master.get(id);
			d.change(l(PageChange.remove(Fruitbat.FULLTEXT_PREFIX + "a")));
			m.reboot();
			d = m.getStore().get(id);
			assertEquals(0, m.getStore().getFullTextIndex().query(l(l("Thessaloniki")),
					l(d)).size());
		} finally {
			m.tearDown();
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
			fw.close();
		}
		return f;
	}
}
