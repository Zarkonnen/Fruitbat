package com.metalbeetle.fruitbat.csvstorage;

import java.io.File;
import java.util.HashMap;
import com.mindprod.csv.CSVWriter;
import com.mindprod.csv.CSVReader;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

final class KVFile {
	private static final String EOL = "EOL";

	final File f;
	private final HashMap<String, String> keyValueMap = new HashMap<String, String>();

	private boolean loaded = false;

	KVFile(File f, Map<String, String> defaults) { this.f = f; keyValueMap.putAll(defaults); }
	KVFile(File f) { this(f, Collections.<String, String>emptyMap()); }

	/** @return The key/value map in the file. Never use keyValueMap directly! */
	HashMap<String, String> kv() {
		if (!loaded) {
			if (f.exists()) {
				CSVReader r = null;
				try {
					r = new CSVReader(
							new BufferedReader(
								new InputStreamReader(
									new FileInputStream(f),
									"UTF-8")
							),
							/*separator*/ ',', /*quote*/ '"', /*multiline*/ false, /*trim*/ true);
					while (true) {
						String[] fs = r.getAllFieldsInLine();
						if (fs.length < 2) { continue; }
						if (!fs[fs.length - 1].equals(EOL)) { continue; }
						if (fs.length == 2) {
							keyValueMap.remove(fs[0]);
						} else {
							keyValueMap.put(fs[0], fs[1]);
						}
					}
				} catch (Exception e) {
					if (!(e instanceof EOFException)) {
						throw new RuntimeException("Couldn't read data from " + f + ".", e);
					}
				} finally {
					try { r.close(); } catch (Exception e) {}
				}
			}
			loaded = true;
		}
		return keyValueMap;
	}

	String get(String key) {
		if (has(key)) { return kv().get(key); }
		throw new RuntimeException("Key " + key + " not found in " + f + ".");
	}

	boolean has(String key) {
		return kv().containsKey(key);
	}

	List<String> keys() {
		return immute(kv().keySet());
	}

	void put(String key, String value) {
		if (value == null) {
			kv().remove(key);
		} else {
			kv().put(key, value);
		}

		mkAncestors(f);

		CSVWriter w = null;
		try {
			w = new CSVWriter(
					new OutputStreamWriter(
						new FileOutputStream(f, /*append*/ true),
					"UTF-8"),
					/*quoteLevel*/ 2, /*separator*/ ',', /*quote*/ '"', /*trim*/ true);
			w.put(key);
			if (value != null) { w.put(value); }
			w.put(EOL);
			w.nl();
		} catch (Exception e) {
			throw new RuntimeException("Couldn't append to " + f + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
		}
	}
}
