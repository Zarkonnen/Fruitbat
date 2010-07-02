package com.metalbeetle.fruitbat.atrio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class ATRTest {

	@Test
	public void testSingleField() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ATRWriter w = new ATRWriter(out);
		w.startRecord();
		w.write("kittens");
		w.endRecord();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		System.out.println(new String(out.toByteArray()));
		System.out.println("------------");
		ATRReader r = new ATRReader(in);
		assertEquals("kittens", r.read());
		assertNull(r.read());
		assertTrue(r.endOfRecord());
		assertTrue(r.cleanEndOfRecord());
		assertFalse(r.endOfStream());
		assertNull(r.read());
		assertTrue(r.endOfStream());
	}

	@Test
	public void testRecords() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ATRWriter w = new ATRWriter(out);
		w.writeRecord(Collections.EMPTY_LIST);
		w.writeRecord(l("kittens", "puppies"));
		w.writeRecord(l("iguanas:%"));
		w.writeRecord(Collections.EMPTY_LIST);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		System.out.println(new String(out.toByteArray()));
		System.out.println("------------");
		ATRReader r = new ATRReader(in);
		List<String> rec = r.readRecord();
		assertEquals(0, rec.size());
		rec = r.readRecord();
		assertEquals(2, rec.size());
		assertEquals("kittens", rec.get(0));
		assertEquals("puppies", rec.get(1));
		rec = r.readRecord();
		assertEquals(1, rec.size());
		assertEquals("iguanas:%", rec.get(0));
		rec = r.readRecord();
		assertEquals(0, rec.size());
		assertNull(r.readRecord());
	}

	@Test
	public void testAbortedRecordsIgnored() throws IOException {
		String data =
				"gibberish" + // Initial gibberish
				"\n:a\t%" + // Full record containing a
				"gibberish" + // More gibberish
				"\n:x:b\t:x%" + // Record containing b, aborted xes
				"\n:x\t" + // Aborted record containing nonaborted x
				"\n:\\u00abcd\t%" + // Full record containing \uabcd
				"\n"; // Aborted record start
		System.out.println(data);
		System.out.println("------------");
		ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());
		ATRReader r = new ATRReader(in);
		List<String> rec;
		rec = r.readRecord();
		assertEquals(1, rec.size());
		assertEquals("a", rec.get(0));
		rec = r.readRecord();
		assertEquals(1, rec.size());
		assertEquals("b", rec.get(0));
		rec = r.readRecord();
		assertEquals(1, rec.size());
		assertEquals("\uabcd", rec.get(0));
		rec = r.readRecord();
		assertNull(rec);
	}
}