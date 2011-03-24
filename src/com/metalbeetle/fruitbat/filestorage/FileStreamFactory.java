package com.metalbeetle.fruitbat.filestorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

/** Allows for plugging in different ways of getting input/output streams for files. */
public interface FileStreamFactory {
	public InputStream inputStream(File f) throws FileNotFoundException;
	public OutputStream outputStream(File f) throws FileNotFoundException;
	public OutputStream outputStream(File f, boolean append) throws FileNotFoundException;
}
