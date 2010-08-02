package com.metalbeetle.fruitbat.util;

import java.util.HashMap;

public class StringPool {
	final HashMap<String, String> pool = new HashMap<String, String>();
	final int cutoff;

	public StringPool(int cutoff) { this.cutoff = cutoff; }

	public String pool(String s) {
		return s.length() > cutoff ? s : poolNoCutoff(s);
	}

	public String poolNoCutoff(String s) {
		if (pool.containsKey(s)) {
			return pool.get(s);
		} else {
			pool.put(s, s);
			return s;
		}
	}
}
