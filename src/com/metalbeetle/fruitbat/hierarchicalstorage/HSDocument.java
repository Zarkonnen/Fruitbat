package com.metalbeetle.fruitbat.hierarchicalstorage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.PageChange;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Document stored on some hierarchical storage medium. */
public class HSDocument implements Comparable<HSDocument>, Document {
	static final String DATA_PREFIX = "d ";
	static final String FILE_PREFIX = "f ";
	static final String CHECKSUM_PREFIX = "c ";
	static final String REVISION_KEY = "rev";

	final int id;
	final Location location;
	final KVFile data;
	final HSStore s;

	/** File names in use by internal data that mustn't be used to store pages. */
	private static final List<String> FORBIDDEN_FILE_NAMES = l("data.atr");

	public HSDocument(Location location, HSStore s) throws FatalStorageException {
		this.location = location;
		this.s = s;
		id = integer(location.getName());
		data = location.child("data.atr").kvFile();
	}

	public int getID() { return id; }

	public String getRevision() throws FatalStorageException {
		String cached = myIndex().getCachedRevision(this);
		if (cached != null) { return cached; }
		try {
			return data.get(REVISION_KEY);
		} catch (Exception e) {
			throw new FatalStorageException("Could not find revision information for document " +
					"with ID " + id + ". The document's data may be corrupt.", e);
		}
	}

	HSIndex myIndex() { return (HSIndex) s.getIndex(); }

	public String change(List<? extends Change> changes) throws FatalStorageException {
		return change(
				Long.toHexString(System.currentTimeMillis()) +
				Integer.toHexString(changes.hashCode()),
				changes);
	}

	public String change(String revision, List<? extends Change> changes) throws FatalStorageException {
		s.updateRevision();
		// Transduce into changes for data.atr.
		ArrayList<DataChange> dataChanges = new ArrayList<DataChange>(changes.size());
		dataChanges.add(DataChange.put(REVISION_KEY, revision));
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
				String checksum;
				try {
					checksum = checksum(p.value);
				} catch (IOException e) {
					throw new FatalStorageException("Could not checksum " + p.value + ".", e);
				}
				String name = findFreePageName(p.value);
				Location newL = location.child(name);
				try {
					newL.put(p.value);
				} catch (Exception e) {
					throw new FatalStorageException("Couldn't store page at " + p.key + ".", e);
				}
				dataChanges.add(DataChange.put(FILE_PREFIX + p.key, name));
				dataChanges.add(DataChange.put(CHECKSUM_PREFIX + p.key, checksum));
				continue;
			}
			if (c instanceof PageChange.Move) {
				PageChange.Move m = (PageChange.Move) c;
				dataChanges.add(DataChange.move(FILE_PREFIX + m.srcKey, FILE_PREFIX + m.dstKey));
				dataChanges.add(DataChange.move(CHECKSUM_PREFIX + m.srcKey, CHECKSUM_PREFIX + m.dstKey));
				continue;
			}
			if (c instanceof PageChange.Remove) {
				PageChange.Remove r = (PageChange.Remove) c;
				dataChanges.add(DataChange.remove(FILE_PREFIX + r.key));
				dataChanges.add(DataChange.remove(CHECKSUM_PREFIX + r.key));
				continue;
			}
		}
		// If we are removing fulltext data, we need to tell the index before we change the data.
		if (s.getFullTextIndex() != null) {
			for (Change c : changes) {
				if (c instanceof PageChange.Remove) {
					PageChange.Remove r = (PageChange.Remove) c;
					if (r.key.startsWith(Fruitbat.FULLTEXT_PREFIX)) {
						s.getFullTextIndex().pageRemoved(getPage(r.key), this);
					}
				}
			}
		}
		// Commit the changes to disk.
		data.change(dataChanges);
		// Inform the index of data/revision changes.
		myIndex().revisionChanged(this, revision);
		for (Change c : changes) {
			if (c instanceof DataChange.Put) {
				DataChange.Put p = (DataChange.Put) c;
				myIndex().keyAdded(this, p.key, p.value);
				continue;
			}
			if (c instanceof DataChange.Move) {
				DataChange.Move m = (DataChange.Move) c;
				myIndex().keyRemoved(this, m.srcKey);
				myIndex().keyAdded(this, m.dstKey, get(m.dstKey));
				continue;
			}
			if (c instanceof DataChange.Remove) {
				DataChange.Remove r = (DataChange.Remove) c;
				myIndex().keyRemoved(this, r.key);
				continue;
			}
			if (s.getFullTextIndex() != null) {
				if (c instanceof PageChange.Put) {
					PageChange.Put p = (PageChange.Put) c;
					if (p.key.startsWith(Fruitbat.FULLTEXT_PREFIX)) {
						s.getFullTextIndex().pageAdded(p.value, this);
					}
					continue;
				}
				if (c instanceof PageChange.Move) {
					PageChange.Move m = (PageChange.Move) c;
					if (m.srcKey.startsWith(Fruitbat.FULLTEXT_PREFIX) &&
						!m.dstKey.startsWith(Fruitbat.FULLTEXT_PREFIX))
					{
						s.getFullTextIndex().pageRemoved(getPage(m.dstKey), this);
					}
					if (!m.srcKey.startsWith(Fruitbat.FULLTEXT_PREFIX) &&
						m.dstKey.startsWith(Fruitbat.FULLTEXT_PREFIX))
					{
						s.getFullTextIndex().pageAdded(getPage(m.dstKey), this);
					}
				}
			}
		}
		return revision;
	}

	public String get(String key) throws FatalStorageException {
		String cached = myIndex().getCachedValue(this, key);
		return cached == null ? data.get(DATA_PREFIX + key) : cached;
	}

	public boolean has(String key) {
		return myIndex().hasKey(this, key);
	}
	public List<String> keys() {
		return myIndex().getKeys(this);
	}

	public DataSrc getPage(String key) throws FatalStorageException {
		return location.child(data.get(FILE_PREFIX + key));
	}

	public String getPageChecksum(String key) throws FatalStorageException {
		return data.get(CHECKSUM_PREFIX + key);
	}

	private String findFreePageName(DataSrc src) throws FatalStorageException {
		// Replace all nonword non-period chars.
		String name = src.getName().replaceAll("[^\\w.]", "");
		if (name.length() > 30) {
			name = name.substring(name.length() - 30, name.length());
		}
		int i = 2;
		int dotIndex = name.lastIndexOf(".");
		String preDot  = dotIndex == -1 ? name : name.substring(0, dotIndex);
		String postDot = dotIndex == -1 ? ""   : name.substring(dotIndex);
		while (FORBIDDEN_FILE_NAMES.contains(name) || location.child(name).exists()) {
			name = preDot + "_" + i + postDot;
			i++;
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

	public int compareTo(HSDocument d2) {
		return id - d2.id;
	}
}
