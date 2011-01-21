package com.metalbeetle.fruitbat.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Data source backed by a byte array. */
public class ByteArraySrc implements DataSrc {
	private final byte[] array;
	private final String name;

	public ByteArraySrc(byte[] array, String name) { this.array = array; this.name = name; }

	public String getName() { return name; }

	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(array);
	}

	public long getLength() {
		return array.length;
	}
}
