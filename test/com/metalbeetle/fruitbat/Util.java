package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.io.DataSrc;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

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

	public static File createFile(String contents) throws IOException {
		File f = File.createTempFile("tmp", ".txt");
		BufferedWriter w = new BufferedWriter(new FileWriter(f));
		w.write(contents);
		w.flush();
		w.close();
		return f;
	}

	public static String getFirstLine(File f) throws FileNotFoundException, IOException {
		BufferedReader r = new BufferedReader(new FileReader(f));
		String l = r.readLine();
		r.close();
		return l;
	}

	public static boolean hasFirstLine(DataSrc src, String contents) throws Exception {
		BufferedReader r = null;
		try {
			return (r = new BufferedReader(new InputStreamReader(src.getInputStream()))).readLine().equals(contents);
		} finally {
			try { r.close(); } catch (Exception e) {}
		}
	}
}
