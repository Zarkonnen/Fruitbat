package com.metalbeetle.fruitbat.util;

import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.io.FileSrc;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.zip.CRC32;
import org.apache.commons.io.FileUtils;

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

	static int BLOCK_SIZE = 2048;
	public static void srcToFile(DataSrc src, File target) throws IOException {
		if (src instanceof FileSrc) {
			try {
				FileUtils.copyFile(((FileSrc) src).f, target);
				return;
			} catch (IOException e) { /* try manually */ }
		}
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			in = new BufferedInputStream(src.getInputStream());
			out = new BufferedOutputStream(new FileOutputStream(target));
			int bytesRead = -1;
			byte[] buffer = new byte[BLOCK_SIZE];
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
		} finally {
			try { in.close(); } catch (Exception e) {}
			out.close();
		}
	}

	public static String checksum(DataSrc src) throws IOException {
		CRC32 crc = new CRC32();
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(src.getInputStream());
			int bytesRead = -1;
			byte[] buffer = new byte[BLOCK_SIZE];
			while ((bytesRead = in.read(buffer)) != -1) {
				crc.update(buffer, 0, bytesRead);
			}
			return Long.toHexString(crc.getValue());
		} finally {
			try { in.close(); } catch (Exception e) {}
		}
	}

	public static String getFullMessage(Throwable t) {
		StringBuilder sb = new StringBuilder();
		while (t != null) {
			if (t.getMessage() != null) { sb.append(t.getMessage()); sb.append("\n"); }
			t = t.getCause();
		}
		return sb.toString();
	}
}
