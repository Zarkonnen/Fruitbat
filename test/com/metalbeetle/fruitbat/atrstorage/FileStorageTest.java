package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.PageChange;
import com.metalbeetle.fruitbat.util.StringPool;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class FileStorageTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	ATRStore s;
	ATRDocIndex i;

	@Test
	public void testStoreAndRetrieve() throws Exception {
		File myF = mkTmpFile("foo");
		Document d = s.create();
		int id = d.getID();
		d.change(l(PageChange.put(SCARY, myF)));
		rebootStore();
		d = s.get(id);
		myF = new File(d.getPage(SCARY).getPath());
		assertTrue(hasContents(myF, "foo"));
	}

	@Test
	public void testDelete() throws Exception {
		File myF = mkTmpFile("foo");
		Document d = s.create();
		int id = d.getID();
		d.change(l(PageChange.put(SCARY, myF)));
		rebootStore();
		d = s.get(id);
		d.change(l(PageChange.remove(SCARY)));
		rebootStore();
		d = s.get(id);
		assertFalse(d.hasPage(SCARY));
	}

	File mkTmpFile(String contents) throws Exception {
		File f = File.createTempFile("foo", "bar");
		FileWriter fw = null;
		try {
			fw = new FileWriter(f);
			fw.write(contents);
		} finally {
			fw.close();
		}
		return f;
	}

	boolean hasContents(File f, String contents) throws Exception {
		BufferedReader r = null;
		try {
			return (r = new BufferedReader(new FileReader(f))).readLine().equals(contents);
		} finally {
			r.close();
		}
	}

	void rebootStore() throws FatalStorageException {
		s.close();
		s = new ATRStore(s.getLocation(), new DummyProgressMonitor());
		i = s.index;
	}

    @Before
    public void setUp() throws FatalStorageException {
		s = new ATRStore(Util.createTempFolder(), new DummyProgressMonitor());
		i = s.index;
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(s.getLocation());
    }
}