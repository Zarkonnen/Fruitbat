package com.metalbeetle.fruitbat.s3storage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import com.metalbeetle.fruitbat.hierarchicalstorage.Location;
import com.metalbeetle.fruitbat.io.DataSink.CommittableOutputStream;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class LocationKVFile implements KVFile {
	final Location l;
	final HashMap<String, String> defaults;
	final HashMap<String, String> __kv = new HashMap<String, String>();
	boolean loaded = false;

	public LocationKVFile(Location l, HashMap<String, String> defaults) {
		this.l = l;
		this.defaults = defaults;
	}

	HashMap<String, String> kv() throws FatalStorageException {
		load();
		return __kv;
	}

	void load() throws FatalStorageException {
		if (!loaded) {
			__kv.clear();
			__kv.putAll(defaults);
			if (l.exists()) {
				ATRReader r = null;
				try {
					r = new ATRReader(l.getInputStream());
					String[] kv = new String[2];
					while (r.readRecord(kv, 0, 2) != -1) {
						__kv.put(kv[0], kv[1]);
					}
				} catch (Exception e) {
					throw new FatalStorageException("Could not read data from " + l.getName() + ".",
							e);
				} finally {
					try { r.close(); } catch (Exception e) {}
				}
				/*SimpleATRReader r = null;
				try {
					r = new SimpleATRReader(l.getInputStream());
					List<String> rec = null;
					while ((rec = r.readRecord()) != null) {
						__kv.put(rec.get(0), rec.get(1));
					}
				} catch (Exception e) {
					throw new FatalStorageException("Could not read data from " + l.getName() + ".",
							e);
				} finally {
					try { r.close(); } catch (Exception e) {}
				}*/
			}
			loaded = true;
		}
	}

	void save() throws FatalStorageException {
		if (!loaded) { return; }
		CommittableOutputStream cos = null;
		ATRWriter w = null;
		try {
			cos = l.getOutputStream();
			w = new ATRWriter(cos.stream());
			for (Entry<String, String> kv : __kv.entrySet()) {
				w.startRecord();
				w.write(kv.getKey());
				w.write(kv.getValue());
				w.endRecord();
			}
		} catch (Exception e) {
			cos.abort();
			throw new FatalStorageException("Unable to save data to " + l.getName() + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
			try {
				cos.commitIfNotAborted();
			} catch (Exception e) {
				throw new FatalStorageException("Unable to save data to " + l.getName() + ".", e);
			}
		}
	}

	public boolean has(String key) throws FatalStorageException {
		return kv().containsKey(key);
	}

	public String get(String key) throws FatalStorageException {
		if (has(key)) {
			return kv().get(key);
		} else {
			throw new FatalStorageException("Unknown key: " + key + " in " + l.getName() + ".");
		}
	}

	public Collection<String> keys() throws FatalStorageException {
		return kv().keySet();
	}

	public void change(List<DataChange> changes) throws FatalStorageException {
		if (changes.size() > 0) {
			for (DataChange c : changes) {
				if (c instanceof DataChange.Put) {
					DataChange.Put p = (DataChange.Put) c;
					kv().put(p.key, p.value);
					continue;
				}
				if (c instanceof DataChange.Move) {
					DataChange.Move m = (DataChange.Move) c;
					if (kv().containsKey(m.srcKey)) {
						kv().put(m.dstKey, kv().get(m.srcKey));
						kv().remove(m.srcKey);
					} else {
						loaded = false;
						throw new FatalStorageException("Could not move key \"" + m.srcKey + "\" " +
								"to \"" + m.dstKey + "\": no mapping exists.");
					}
					continue;
				}
				if (c instanceof DataChange.Remove) {
					DataChange.Remove r = (DataChange.Remove) c;
					kv().remove(r.key);
				}
			}
			save();
		}
	}

	public void saveToCache() throws FatalStorageException {
		if (loaded) {
			save();
		}
	}
}
