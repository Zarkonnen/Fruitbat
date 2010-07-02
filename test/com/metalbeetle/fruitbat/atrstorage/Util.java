package com.metalbeetle.fruitbat.atrstorage;

import java.io.File;
import java.io.IOException;

public final class Util {
	private Util() {}

	public static File createTempFolder() {
		try {
			File f = File.createTempFile("tmp", "");
			f.delete();
			f.mkdirs();
			return f.getAbsoluteFile();
		} catch (IOException e) { throw new RuntimeException(e); }
	}

	public static void deleteRecursively(File f) {
		if (f.isDirectory()) {
			for (File child : f.listFiles()) { deleteRecursively(child); }
		}
		f.delete();
	}
}
