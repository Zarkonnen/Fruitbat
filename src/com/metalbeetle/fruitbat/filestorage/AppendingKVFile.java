package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
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
public final class AppendingKVFile implements KVFile {
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

	public AppendingKVFile(File f, File cacheF,  Map<String, String> defaults) {
		this.f = f; this.cacheF = cacheF; this.defaults = new HashMap<String, String>(defaults);
	}
	public AppendingKVFile(File f, Map<String, String> defaults) { this(f, null, defaults); }
	public AppendingKVFile(File f) { this(f, Collections.<String, String>emptyMap()); }

	/** @return The key/value map in the file. Never use keyValueMap directly! */
	HashMap<String, String> kv() throws FatalStorageException {
		load();
		return keyValueMap;
	}

	/** @return The sorted list of keys in the file. Never use keys directly! */
	TreeSet<String> k() throws FatalStorageException {
		load();
		return keys;
	}

	private void load() throws FatalStorageException {
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
						keyValueMap.put(fields[0], fields[1]);
						keys.add(fields[0]);
					}
				} catch (Exception e) {
					// Who cares?
				} finally {
					try { r.close(); } catch (Exception e) {}
				}
				if (!cacheF.delete()) {
					throw new FatalStorageException("Cannot delete temporary cache file at " +
							cacheF + ".");
				}
			}
			if (!loaded && f.exists()) {
				keyValueMap.clear();
				keys.clear();
				keyValueMap.putAll(defaults);
				keys.addAll(defaults.keySet());
				ATRReader r = null;
				try {
					r = new ATRReader(new BufferedInputStream(new FileInputStream(f)));
					List<String> rec;
					while ((rec = r.readRecord()) != null) {
						int i = 0;
						while (i < rec.size()) {
							if (rec.get(i).equals(PUT)) {
								keyValueMap.put(rec.get(i + 1), rec.get(i + 2));
								keys.add(rec.get(i + 1));
								i += 3;
								continue;
							}
							if (rec.get(i).equals(MOVE)) {
								String value = keyValueMap.get(rec.get(i + 1));
								if (value != null) {
									keyValueMap.put(rec.get(i + 2), value);
									keys.add(rec.get(i + 2));
									keyValueMap.remove(rec.get(i + 1));
									keys.remove(rec.get(i + 1));
								}
								i += 3;
								continue;
							}
							if (rec.get(i).equals(REMOVE)) {
								keyValueMap.remove(rec.get(i + 1));
								keys.remove(rec.get(i + 1));
								i += 2;
								continue;
							}
						}
					}
				} catch (Exception e) {
					throw new FatalStorageException("Couldn't read data from " + f + ".", e);
				} finally {
					try { r.close(); } catch (Exception e) {}
				}
			}
			loaded = true;
		}
	}

	public void saveToCache() throws FatalStorageException {
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
				throw new FatalStorageException("Couldn't append to " + f + ".", e);
			} finally {
				try { w.close(); } catch (Exception e) {}
			}
		}
	}

	public String get(String key) throws FatalStorageException {
		if (has(key)) { return kv().get(key); }
		throw new FatalStorageException("Key " + key + " not found in " + f + ".");
	}

	public boolean has(String key) throws FatalStorageException {
		return kv().containsKey(key);
	}

	public List<String> keys() throws FatalStorageException {
		return immute(k());
	}

	public void change(List<DataChange> changes) throws FatalStorageException {
		// If there is a cache file, we must load it in to prevent it from hanging around with old
		// data while we blithely write newer data to the non-cache file, getting them out of sync.
		if (cacheF != null) {
			load();
		}
		if (loaded) {
			for (DataChange c : changes) {
				if (c instanceof DataChange.Put) {
					DataChange.Put p = (DataChange.Put) c;
					kv().put(p.key, p.value);
					k().add(p.key);
				}
				if (c instanceof DataChange.Move) {
					DataChange.Move m = (DataChange.Move) c;
					String value = kv().get(m.srcKey);
					if (value == null) { return; }
					kv().put(m.dstKey, value);
					k().add(m.dstKey);
					kv().remove(m.srcKey);
					k().remove(m.srcKey);
				}
				if (c instanceof DataChange.Remove) {
					DataChange.Remove r = (DataChange.Remove) c;
					kv().remove(r.key);
					k().remove(r.key);
				}
			}
		}

		append(changes);
	}

	void append(List<DataChange> changes) throws FatalStorageException {
		mkAncestors(f);

		ATRWriter w = null;
		try {
			w = new ATRWriter(new BufferedOutputStream(new FileOutputStream(f, /*append*/ true)));
			w.startRecord();
			for (DataChange c : changes) {
				if (c instanceof DataChange.Put) {
					DataChange.Put p = (DataChange.Put) c;
					w.write(PUT);
					w.write(p.key);
					w.write(p.value);
				}
				if (c instanceof DataChange.Move) {
					DataChange.Move m = (DataChange.Move) c;
					w.write(MOVE);
					w.write(m.srcKey);
					w.write(m.dstKey);
				}
				if (c instanceof DataChange.Remove) {
					DataChange.Remove r = (DataChange.Remove) c;
					w.write(REMOVE);
					w.write(r.key);
				}
			}
			w.endRecord();
		} catch (Exception e) {
			throw new FatalStorageException("Couldn't append to " + f + ".", e);
		} finally {
			try {
				w.close();
			} catch (Exception e) {
				throw new FatalStorageException("Couldn't append to " + f + ".", e);
			}
		}
	}
}
