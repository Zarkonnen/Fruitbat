package com.metalbeetle.fruitbat.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileSrc implements DataSrc {
	public final File f;
	public FileSrc(File f) {
		if (f == null) {
			throw new NullPointerException("FileSrc object cannot be constructed with a null file.");
		}
		this.f = f;
	}
	public String getName() { return f.getName(); }
	public InputStream getInputStream() throws IOException { return new FileInputStream(f); }
	@Override
	public String toString() { return f.getPath(); }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FileSrc)) { return false; }
		return ((FileSrc) o).f.equals(o);
	}

	@Override
	public int hashCode() { return 2333 + f.hashCode() * 9; }

	public long getLength() {
		return f.length();
	}
}
