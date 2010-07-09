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
import java.util.TreeSet;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

/** Stores key/value data in an append-only ATR file. */
final class KVFile {
	static final String PUT    = "p";
	static final String REMOVE = "r";
	static final String MOVE   = "m";
	static final String END_OF_CACHE = "EOC";

	final File f;
	final File cacheF;
	private final Map<String, String> defaults;
	private final HashMap<String, String> keyValueMap = new HashMap<String, String>();
	private final TreeSet<String> keys = new TreeSet<String>();

	private boolean loaded = false;

	KVFile(File f, File cacheF,  Map<String, String> defaults) {
		this.f = f; this.cacheF = cacheF; this.defaults = new HashMap<String, String>(defaults);
	}
	KVFile(File f, Map<String, String> defaults) { this(f, null, defaults); }
	KVFile(File f) { this(f, Collections.<String, String>emptyMap()); }

	/** @return The key/value map in the file. Never use keyValueMap directly! */
	HashMap<String, String> kv() {
		load();
		return keyValueMap;
	}

	/** @return The sorted list of keys in the file. Never use keys directly! */
	TreeSet<String> k() {
		load();
		return keys;
	}

	private void load() {
		if (!loaded) {
			keyValueMap.clear();
			keys.clear();
			keyValueMap.putAll(defaults);
			keys.addAll(defaults.keySet());
			if (cacheF != null && cacheF.exists()) {
				ATRReader r = null;
				try {
					r = new ATRReader(new BufferedInputStream(new FileInputStream(cacheF)));
					String[] fields = new String[2];
					int fieldsRead;
					while ((fieldsRead = r.readRecord(fields, 0, 2)) != -1) {
						if (fieldsRead == 0) { break; } // Broken cache!
						if (fieldsRead == 1 && fields[0].equals(END_OF_CACHE)) {
							loaded = true;
							break;
						}
						keyValueMap.put(fields[1], fields[2]);
						keys.add(fields[1]);
					}
				} catch (Exception e) {
					throw new RuntimeException("Couldn't read data from " + f + ".", e);
				} finally {
					try { r.close(); } catch (Exception e) {}
				}
				loaded = cacheF.delete() && loaded;
			}
			if (!loaded && f.exists()) {
				keyValueMap.clear();
				keys.clear();
				keyValueMap.putAll(defaults);
				keys.addAll(defaults.keySet());
				ATRReader r = null;
				try {
					r = new ATRReader(new BufferedInputStream(new FileInputStream(f)));
					String[] fields = new String[3];
					int fieldsRead;
					while ((fieldsRead = r.readRecord(fields, 0, 2)) != -1) {
						if (fieldsRead != 3) { continue; }
						if (fields[0].equals(PUT)) {
							keyValueMap.put(fields[1], fields[2]);
							keys.add(fields[1]);
							continue;
						}
						if (fields[0].equals(REMOVE)) {
							keyValueMap.remove(fields[1]);
							keys.remove(fields[1]);
							continue;
						}
						if (fields[0].equals(MOVE)) {
							String value = kv().get(fields[1]);
							if (value == null) { continue; }
							keyValueMap.put(fields[2], value);
							keys.add(fields[2]);
							keyValueMap.remove(fields[1]);
							keys.remove(fields[1]);
							continue;
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
	}

	void saveToCache() {
		if (cacheF != null && loaded) {
			mkAncestors(cacheF);

			ATRWriter w = null;
			try {
				w = new ATRWriter(new BufferedOutputStream(new FileOutputStream(cacheF,
						/*append*/ false)));
				for (Map.Entry<String, String> kv : keyValueMap.entrySet()) {
					w.startRecord();
					w.write(kv.getKey());
					w.write(kv.getValue());
					w.endRecord();
				}
				w.startRecord();
				w.write(END_OF_CACHE);
				w.endRecord();
				loaded = false;
			} catch (Exception e) {
				throw new RuntimeException("Couldn't append to " + f + ".", e);
			} finally {
				try { w.close(); } catch (Exception e) {}
			}
		}
	}
	
	String get(String key) {
		if (has(key)) { return kv().get(key); }
		throw new RuntimeException("Key " + key + " not found in " + f + ".");
	}

	boolean has(String key) {
		return kv().containsKey(key);
	}

	List<String> keys() {
		return immute(k());
	}

	/** Puts the key/value into the file. */
	void put(String key, String value) {
		kv().put(key, value);
		k().add(key);
		append(PUT, key, value);
	}

	/** Removes the mapping from the file. */
	void remove(String key) {
		kv().remove(key);
		k().remove(key);
		append(REMOVE, key, "");
	}

	/** Moves the mapping from one key to another. */
	void move(String srcKey, String dstKey) {
		String value = kv().get(srcKey);
		if (value == null) { return; }
		kv().put(dstKey, value);
		k().add(dstKey);
		kv().remove(srcKey);
		k().remove(srcKey);
		append(MOVE, srcKey, dstKey);
	}

	void append(String action, String arg1, String arg2) {
		mkAncestors(f);

		ATRWriter w = null;
		try {
			w = new ATRWriter(new BufferedOutputStream(new FileOutputStream(f, /*append*/ true)));
			w.startRecord();
			w.write(action);
			w.write(arg1);
			w.write(arg2);
			w.endRecord();
		} catch (Exception e) {
			throw new RuntimeException("Couldn't append to " + f + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
		}
	}
}
