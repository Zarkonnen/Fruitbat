package com.metalbeetle.fruitbat.filestorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class DefaultFileStreamFactory implements FileStreamFactory {
	public InputStream inputStream(File f) throws FileNotFoundException {
		return new FileInputStream(f);
	}

	public OutputStream outputStream(File f) throws FileNotFoundException {
		return new FileOutputStream(f);
	}

	public OutputStream outputStream(File f, boolean append) throws FileNotFoundException {
		return new FileOutputStream(f, append);
	}
}
