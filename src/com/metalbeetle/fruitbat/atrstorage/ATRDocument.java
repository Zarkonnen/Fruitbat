package com.metalbeetle.fruitbat.atrstorage;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.PageChange;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

/** Document stored on file system, using ATR files, which guarantees atomicity. */
class ATRDocument implements Comparable<ATRDocument>, Document {
	static final String DATA_PREFIX = "d ";
	static final String FILE_PREFIX = "f ";

	final int id;
	final File location;
	final KVFile data;
	final ATRStore s;

	/** File names in use by internal data that mustn't be used to store pages. */
	private static final List<String> FORBIDDEN_FILE_NAMES = l("data.atr");

	public ATRDocument(File location, ATRStore s) {
		this.location = location;
		this.s = s;
		id = integer(location.getName());
		data = new KVFile(new File(location, "data.atr"));
	}

	public int getID() { return id; }
	public long getVersion() { return s.index.getCachedVersion(this); }

	public void change(List<Change> changes) throws FatalStorageException {
		// Transduce into changes for data.atr.
		ArrayList<Change> dataChanges = new ArrayList<Change>(changes.size());
		for (Change c : changes) {
			if (c instanceof DataChange.Put) {
				DataChange.Put p = (DataChange.Put) c;
				dataChanges.add(DataChange.put(DATA_PREFIX + p.key, p.value));
				continue;
			}
			if (c instanceof DataChange.Move) {
				DataChange.Move m = (DataChange.Move) c;
				dataChanges.add(DataChange.move(DATA_PREFIX + m.srcKey, DATA_PREFIX + m.dstKey));
				continue;
			}
			if (c instanceof DataChange.Remove) {
				DataChange.Remove r = (DataChange.Remove) c;
				dataChanges.add(DataChange.remove(DATA_PREFIX + r.key));
				continue;
			}
			if (c instanceof PageChange.Put) {
				PageChange.Put p = (PageChange.Put) c;
				String name = findFreePageName(p.value);
				File newF = new File(location, name);
				mkAncestors(newF);
				try {
					FileUtils.copyFile(p.value, newF);
				} catch (IOException e) {
					throw new FatalStorageException("Couldn't store page at " + p.key + ".\n" +
							"Can't copy " + p.value + " to " + newF + ".", e);
				}
				dataChanges.add(DataChange.put(FILE_PREFIX + p.key, name));
				continue;
			}
			if (c instanceof PageChange.Move) {
				PageChange.Move m = (PageChange.Move) c;
				dataChanges.add(DataChange.move(FILE_PREFIX + m.srcKey, FILE_PREFIX + m.dstKey));
				continue;
			}
			if (c instanceof PageChange.Remove) {
				PageChange.Remove r = (PageChange.Remove) c;
				dataChanges.add(DataChange.remove(FILE_PREFIX + r.key));
				continue;
			}
		}
		// Commit the changes to disk.
		data.change(dataChanges);
		// Inform the index of data changes.
		for (Change c : changes) {
			if (c instanceof DataChange.Put) {
				DataChange.Put p = (DataChange.Put) c;
				s.index.keyAdded(this, p.key, p.value);
				continue;
			}
			if (c instanceof DataChange.Move) {
				DataChange.Move m = (DataChange.Move) c;
				s.index.keyRemoved(this, m.srcKey);
				s.index.keyAdded(this, m.dstKey, get(m.dstKey));
				continue;
			}
			if (c instanceof DataChange.Remove) {
				DataChange.Remove r = (DataChange.Remove) c;
				s.index.keyRemoved(this, r.key);
				continue;
			}
		}
		s.index.nextVersion(this);
	}

	public String get(String key) throws FatalStorageException {
		String cached = s.index.getCachedValue(this, key);
		return cached == null ? data.get(DATA_PREFIX + key) : cached;
	}

	public boolean has(String key) {
		return s.index.hasKey(this, key);
	}
	public List<String> keys() {
		return s.index.getKeys(this);
	}

	public URI getPage(String key) throws FatalStorageException {
		return new File(location, data.get(FILE_PREFIX + key)).toURI();
	}

	private String findFreePageName(File f) {
		String name = f.getName();
		int i = 2;
		int dotIndex = name.lastIndexOf(".");
		String preDot  = dotIndex == -1 ? name : name.substring(0, dotIndex);
		String postDot = dotIndex == -1 ? ""   : name.substring(dotIndex);
		while (FORBIDDEN_FILE_NAMES.contains(name) || new File(location, name).exists()) {
			name = preDot + "_" + i + postDot;
		}
		return name;
	}

	public boolean hasPage(String key) throws FatalStorageException {
		return data.has(FILE_PREFIX + key);
	}

	public List<String> pageKeys() throws FatalStorageException {
		ArrayList<String> pks = new ArrayList<String>();
		for (String k : data.keys()) {
			if (k.startsWith(FILE_PREFIX)) {
				pks.add(k.substring(FILE_PREFIX.length()));
			}
		}
		return pks;
	}

	@Override
	public String toString() {
		return "doc@" + location;
	}

	public int compareTo(ATRDocument d2) {
		return id - d2.id;
	}
}
