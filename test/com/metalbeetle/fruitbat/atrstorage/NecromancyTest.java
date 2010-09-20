/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.Util;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.SearchResult;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

/**
 *
 * @author zar
 */
public class NecromancyTest {
	public static final String SCARY = "\"\n\r\u0026\u0416\u4E2D\uD800\uDF46\n\n\"\n\"\"\"\t\r\"\n";

	ATRStore s;
	ATRDocIndex i;

	@Test
	public void modifyWhileDeleted() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		int id = d1.getID();
		s.delete(d1);

		rebootStore();

		d1 = s.getDeleted(id);
		d1.change(l(DataChange.put("a", "y")));

		d1 = s.undelete(id);

		assertEquals("y", d1.get("a"));
	}

	@Test
	public void getWhileDeleted() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		int id = d1.getID();
		s.delete(d1);
		rebootStore();
		d1 = s.getDeleted(id);
		d1.change(l(DataChange.put("a", "y")));
		assertEquals("y", d1.get("a"));
	}

	@Test
	public void noSearchWhileDeleted() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		int id = d1.getID();
		s.delete(d1);
		rebootStore();
		d1 = s.getDeleted(id);
		d1.change(l(DataChange.put("a", "y")));

		SearchResult result;
		result = i.search(m(p("a", "y")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());
		result = i.search(m(p("a", "x")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());
		result = i.search(m(p("a", "")), DocIndex.ALL_DOCS);
		assertEquals(0, result.docs.size());

		s.undelete(id);

		result = i.search(m(p("a", "y")), DocIndex.ALL_DOCS);
		assertEquals(1, result.docs.size());
	}

	@Test
	public void correctChangeIDAfterCrash() throws FatalStorageException {
		Document d1 = s.create();
		d1.change(l(DataChange.put("a", "x")));
		d1.change(l(DataChange.put("a", "y")));
		int id1 = d1.getID();
		String v1 = d1.getRevision();
		Document d2 = s.create();
		d2.change(l(DataChange.put("a", "x")));
		d2.change(l(DataChange.put("a", "y")));
		d2.change(l(DataChange.put("a", "z")));
		int id2 = d2.getID();
		String v2 = d2.getRevision();
		s.delete(d1);
		crashAndRebootStore();
		assertEquals(v1, s.getDeleted(id1).getRevision());
		assertEquals(v2, s.get(id2).getRevision());
	}

	void rebootStore() throws FatalStorageException {
		i.close();
		s = new ATRStore(s.getLocation(), new DummyProgressMonitor());
		i = (ATRDocIndex) s.getIndex();
	}

	void crashAndRebootStore() throws FatalStorageException {
		s = new ATRStore(s.getLocation(), new DummyProgressMonitor());
		i = (ATRDocIndex) s.getIndex();
	}

    @Before
    public void setUp() throws FatalStorageException {
		s = new ATRStore(Util.createTempFolder(), new DummyProgressMonitor());
		i = (ATRDocIndex) s.getIndex();
    }

    @After
    public void tearDown() {
		Util.deleteRecursively(s.getLocation());
    }
}