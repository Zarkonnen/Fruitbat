package com.metalbeetle.fruitbat.util;

import com.metalbeetle.fruitbat.Fruitbat;
import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.GregorianCalendar;

/** Misc utilities. */
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

	public static boolean nullAwareEquals(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}

	/** Creates the ancestral directories of the given file, throwing an exception on failure. */
	public static void mkAncestors(File f) {
		File parent = f.getAbsoluteFile().getParentFile();
		if (!parent.exists()) {
			if (!parent.mkdirs()) {
				throw new RuntimeException("Couldn't create parent folder for " + f + ".");
			}
		}
	}

	/** Like File.mkdirs(), but throws an exception on failure to create. */
	public static void mkDirs(File f) {
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Couldn't create folder at " + f + ".");
			}
		}
	}

	public static String currentDateString() {
		GregorianCalendar d = new GregorianCalendar();
		String ds = string(d.get(GregorianCalendar.YEAR));
		String month = string(d.get(GregorianCalendar.MONTH) + 1);
		ds += month.length() == 1 ? "-0" + month : month;
		String day = string(d.get(GregorianCalendar.DAY_OF_MONTH));
		ds += day.length() == 1 ? "-0" + day : "-" + day;
		return ds;
	}
}
