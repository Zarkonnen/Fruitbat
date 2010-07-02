package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import java.io.File;
import java.util.HashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

/** Stores key/value data in an append-only ATR file. */
final class KVFile {
	final File f;
	private final HashMap<String, String> keyValueMap = new HashMap<String, String>();

	private boolean loaded = false;

	KVFile(File f, Map<String, String> defaults) { this.f = f; keyValueMap.putAll(defaults); }
	KVFile(File f) { this(f, Collections.<String, String>emptyMap()); }

	/** @return The key/value map in the file. Never use keyValueMap directly! */
	HashMap<String, String> kv() {
		if (!loaded) {
			if (f.exists()) {
				ATRReader r = null;
				try {
					r = new ATRReader(new BufferedInputStream(new FileInputStream(f)));
					String[] fields = new String[2];
					int fieldsRead;
					while ((fieldsRead = r.readRecord(fields, 0, 2)) != -1) {
						switch (fieldsRead) {
							case 2: keyValueMap.put(fields[0], fields[1]); break;
							case 1: keyValueMap.remove(fields[0]);         break;
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Couldn't read data from " + f + ".", e);
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

	/** Puts the key/value into the file - use a value of null to remove a mapping. */
	void put(String key, String value) {
		if (value == null) {
			kv().remove(key);
		} else {
			kv().put(key, value);
		}

		mkAncestors(f);

		ATRWriter w = null;
		try {
			// Append a record to the ATR file. If it's a put, write both the key and the value. If
			// it's a removal, write only the key.
			w = new ATRWriter(new BufferedOutputStream(new FileOutputStream(f, /*append*/ true)));
			w.startRecord();
			w.write(key);
			if (value != null) { w.write(value); }
			w.endRecord();
		} catch (Exception e) {
			throw new RuntimeException("Couldn't append to " + f + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
		}
	}
}
