package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.TestStoreManager;
import com.metalbeetle.fruitbat.TestStoreManagers;
import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.io.FileSrc;
import java.io.File;
import java.io.FileWriter;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class PageStorageTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	@Test
	public void testStoreAndRetrieve() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				File myF = mkTmpFile("foo");
				Document d = m.getStore().create();
				int id = d.getID();
				d.change(l(PageChange.put(SCARY, new FileSrc(myF))));
				m.reboot();
				d = m.getStore().get(id);
				assertTrue(Util.hasFirstLine(d.getPage(SCARY), "foo"));
			} finally {
				m.tearDown();
			}
		}
	}

	@Test
	public void testDelete() throws Exception {
		for (TestStoreManager m : TestStoreManagers.get()) {
			try {
				m.setUp();
				File myF = mkTmpFile("foo");
				Document d = m.getStore().create();
				int id = d.getID();
				d.change(l(PageChange.put(SCARY, new FileSrc(myF))));
				m.reboot();
				d = m.getStore().get(id);
				d.change(l(PageChange.remove(SCARY)));
				m.reboot();
				d = m.getStore().get(id);
				assertFalse(d.hasPage(SCARY));
			} finally {
				m.tearDown();
			}
		}
	}

	File mkTmpFile(String contents) throws Exception {
		File f = File.createTempFile("foo", "bar");
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