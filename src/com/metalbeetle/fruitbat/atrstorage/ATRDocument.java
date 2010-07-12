package com.metalbeetle.fruitbat.atrstorage;
import com.metalbeetle.fruitbat.storage.Document;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

import java.io.File;
import java.net.URI;
import java.util.List;

/** Document stored on file system, using ATR files, which guarantees atomicity. */
class ATRDocument implements Comparable<ATRDocument>, Document {
	final int id;
	final File location;
	final KVFile data;
	final KVFile files;
	final ATRStore s;

	/** File names in use by internal data that mustn't be used to store pages. */
	private static final List<String> FORBIDDEN_FILE_NAMES = l("data.atr", "files.atr");

	public ATRDocument(File location, ATRStore s) {
		this.location = location;
		this.s = s;
		id = integer(location.getName());
		data = new KVFile(new File(location, "data.atr"));
		files = new KVFile(new File(location, "files.atr"));
	}

	public String getID() { return string(id); }

	public String get(String key) {
		String cached = s.keyIndex.getCachedValue(this, key);
		return cached == null ? data.get(key) : cached;
	}

	public void put(String key, String value) {
		data.put(key, value);
		s.keyIndex.keyAdded(this, key, value);
	}

	public void remove(String key) {
		data.remove(key);
		s.keyIndex.keyRemoved(this, key);
	}

	public void move(String srcKey, String dstKey) {
		data.move(srcKey, dstKey);
		s.keyIndex.keyRemoved(this, srcKey);
		s.keyIndex.keyAdded(this, dstKey, data.get(dstKey));
	}

	public boolean has(String key) {
		return s.keyIndex.hasKey(this, key);
	}
	public List<String> keys() {
		return s.keyIndex.getKeys(this);
	}

	public URI getPage(String key) { return new File(location, files.get(key)).toURI(); }

	public void putPage(String key, File f) {
		String name = findFreePageName(f);
		File newF = new File(location, name);
		mkAncestors(newF);
		if (!f.renameTo(newF)) {
			throw new RuntimeException("Couldn't store page at " + key + ".\n" +
					"Can't move " + f + " to " + newF + ".");
		}
		// Until the next call completes, the page hasn't been added, preserving atomicity.
		files.put(key, name);
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

	public void removePage(String key) {
		files.remove(key);
	}

	public void movePage(String srcKey, String dstKey) {
		files.move(srcKey, dstKey);
	}

	public boolean hasPage(String key) { return files.has(key); }
	public List<String> pageKeys() { return files.keys(); }

	@Override
	public String toString() {
		return "doc@" + location;
	}

	public int compareTo(ATRDocument d2) {
		return id - d2.id;
	}
}
