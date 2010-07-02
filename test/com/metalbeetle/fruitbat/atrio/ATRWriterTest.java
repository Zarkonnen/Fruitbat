package com.metalbeetle.fruitbat.atrio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class ATRWriterTest {
	@Test
	public void ascii() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ATRWriter w = new ATRWriter(out);
		w.write("Hello World!");
		assertEquals(":Hello World!\t", new String(out.toByteArray()));
	}

	@Test
	public void escapables() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ATRWriter w = new ATRWriter(out);
		w.write(":%\n\r\t\\\uabcd");
		assertEquals(":\\c\\p\\n\\r\\t\\\\\\u00abcd\t", new String(out.toByteArray()));
	}

	@Test
	public void fieldsAndRecords() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ATRWriter w = new ATRWriter(out);
		w.startRecord();
		w.write("\n\r\t\\\uabcd");
		w.write("x");
		w.endRecord();
		w.startRecord();
		w.write("y");
		w.endRecord();
		assertEquals("\n:\\n\\r\\t\\\\\\u00abcd\t:x\t%\n:y\t%", new String(out.toByteArray()));
	}
}