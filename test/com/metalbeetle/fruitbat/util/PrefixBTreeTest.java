/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metalbeetle.fruitbat.util;

import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author zar
 */
public class PrefixBTreeTest {
	@Test
	public void storeAndRetrieve() {
		PrefixBTree<String> t = new PrefixBTree<String>();
		t.put("key", "value");
		HashSet<String> l = t.get("key");
		assertEquals(1, l.size());
		assertTrue(l.contains("value"));
	}

	@Test
	public void prefixStoreAndRetrieve() {
		PrefixBTree<String> t = new PrefixBTree<String>();
		t.put("key", "value");
		HashSet<String> l = t.get("ke");
		assertEquals(1, l.size());
		assertTrue(l.contains("value"));
	}

	@Test
	public void prefixMultiStoreAndRetrieve() {
		PrefixBTree<String> t = new PrefixBTree<String>();
		t.put("key", "value");
		t.put("keyharaissa", "balue");
		t.put("fruitbat", "kumquat");
		HashSet<String> l;
		l = t.get("ke");
		assertEquals(2, l.size());
		assertTrue(l.contains("value"));
		assertTrue(l.contains("balue"));
		assertFalse(l.contains("kumquat"));
		l = t.get("key");
		assertEquals(2, l.size());
		assertTrue(l.contains("value"));
		assertTrue(l.contains("balue"));
		assertFalse(l.contains("kumquat"));
		l = t.get("keyh");
		assertEquals(1, l.size());
		assertFalse(l.contains("value"));
		assertTrue(l.contains("balue"));
		assertFalse(l.contains("kumquat"));
		l = t.get("");
		assertEquals(3, l.size());
		assertTrue(l.contains("value"));
		assertTrue(l.contains("balue"));
		assertTrue(l.contains("kumquat"));
		l = t.get("nosuchkey");
		assertEquals(0, l.size());
	}

	@Test
	public void addAndRemove() {
		PrefixBTree<String> t = new PrefixBTree<String>();
		t.put("key", "value");
		t.remove("key", "iguanadon");
		assertEquals(1, t.get("key").size());
		t.remove("key", "value");
		assertEquals(0, t.get("key").size());
	}

	@Test
	public void lotsOfAddAndRemove() {
		PrefixBTree<String> t = new PrefixBTree<String>();
		for (int year = 1990; year <= 2010; year++) {
			for (int month = 1; month <= 9; month++) {
				t.put(year + "-0" + month + "-" + "17", year + "-0" + month + "-" + "17");
			}
		}
		/*System.out.println("Peak number of nodes = " + t.numberOfNodes());
		System.out.println("Peak size = " + t.size());*/
		for (int year = 1990; year <= 2010; year++) {
			for (int month = 1; month <= 9; month += 2) {
				t.remove(year + "-0" + month + "-" + "17", year + "-0" + month + "-" + "17");
			}
		}
		/*System.out.println("Post-delete number of nodes = " + t.numberOfNodes());
		System.out.println("Post-delete size = " + t.size());*/
		assertEquals(0, t.get("1997-03-17").size());
		assertEquals(1, t.get("1997-02-17").size());
		assertEquals(1, t.get("1997-02").size());
		assertEquals(4, t.get("1997").size());
		assertEquals(40, t.get("19").size());
	}
}