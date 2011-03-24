package com.metalbeetle.fruitbat.filestorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IntentionallyCrashingFileStreamFactory implements FileStreamFactory {
	byte crashAtInputByte;
	boolean doCrashInput;
	byte crashAtOutputByte;
	boolean doCrashOutput;

	public void crashAtInput(byte crashAtInputByte) {
		this.crashAtInputByte = crashAtInputByte;
		doCrashInput = true;
	}

	public void crashAtOutput(byte crashAtOutputByte) {
		this.crashAtOutputByte = crashAtOutputByte;
		doCrashOutput = true;
	}

	public InputStream inputStream(File f) throws FileNotFoundException {
		final InputStream fis = new FileInputStream(f);
		return new InputStream() {
			@Override
			public int read() throws IOException {
				int b = fis.read();
				if (doCrashInput && b == crashAtInputByte) {
					doCrashInput = false;
					throw new IOException("Crashing input intentionally at byte value " + b + ".");
				} else {
					return b;
				}
			}
		};
	}

	public OutputStream outputStream(File f) throws FileNotFoundException {
		final OutputStream fos = new FileOutputStream(f);
		return new OutputStream() {
			@Override
			public void write(int i) throws IOException {
				if (doCrashOutput && i == crashAtOutputByte) {
					doCrashOutput = false;
					throw new IOException("Crashing output intentionally at byte value " + i + ".");
				} else {
					fos.write(i);
				}
			}
		};
	}

	public OutputStream outputStream(File f, boolean append) throws FileNotFoundException {
		final OutputStream fos = new FileOutputStream(f, append);
		return new OutputStream() {
			@Override
			public void write(int i) throws IOException {
				if (doCrashOutput && i == crashAtOutputByte) {
					doCrashOutput = false;
					throw new IOException("Crashing output intentionally at byte value " + i + ".");
				} else {
					fos.write(i);
				}
			}
		};
	}
}
