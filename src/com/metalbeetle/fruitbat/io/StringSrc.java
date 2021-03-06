package com.metalbeetle.fruitbat.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Data source of UTF-8 string. */
public class StringSrc implements DataSrc {
	public final String s;
	final String name;

	public StringSrc(String s) { this.s = s; this.name = s; }
	public StringSrc(String s, String name) { this.s = s; this.name = name; }

	public String getName() { return name; }

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(s.getBytes("UTF-8"));
	}

	public long getLength() throws IOException {
		return s.getBytes("UTF-8").length;
	}
}