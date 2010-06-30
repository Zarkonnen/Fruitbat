package com.metalbeetle.fruitbat.util;

import java.io.File;

public final class Misc {
	private Misc() {}

	public static String string(Object o) { return "" + o; }
	public static int integer(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new RuntimeException(s + " is not an integer number.");
		}
	}

	public static void mkAncestors(File f) {
		File parent = f.getAbsoluteFile().getParentFile();
		if (!parent.exists()) {
			if (!parent.mkdirs()) {
				throw new RuntimeException("Couldn't create parent folder for " + f + ".");
			}
		}
	}

	public static void mkDirs(File f) {
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Couldn't create folder at " + f + ".");
			}
		}
	}
}
