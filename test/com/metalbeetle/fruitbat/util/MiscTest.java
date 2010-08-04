/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metalbeetle.fruitbat.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author zar
 */
public class MiscTest {
	@Test
	public void testDownload() throws IOException {
		File f = File.createTempFile("foo", "bar");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		StringBuilder longStringB = new StringBuilder();
		for (int i = 0; i < 3000; i++) { longStringB.append(i); }
		String longString = longStringB.toString();
		bw.write(longString);
		bw.close();
		File f2 = File.createTempFile("foo", "quux");
		Misc.download(f.toURL(), f2);
		BufferedReader r = new BufferedReader(new FileReader(f2));
		assertEquals(longString, r.readLine());
		r.close();
		f.delete();
		f2.delete();
	}
}