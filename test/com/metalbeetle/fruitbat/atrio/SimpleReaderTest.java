package com.metalbeetle.fruitbat.atrio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleReaderTest {
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
		SimpleATRReader r = new SimpleATRReader(in);
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