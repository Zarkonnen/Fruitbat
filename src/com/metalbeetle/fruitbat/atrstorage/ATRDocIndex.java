package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.SearchOutcome;
import com.metalbeetle.fruitbat.storage.SearchResult;
import com.metalbeetle.fruitbat.util.Pair;
import com.metalbeetle.fruitbat.util.PrefixBTree;
import com.metalbeetle.fruitbat.util.StringPool;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.LinkedList;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

/** Key index for ATRStore. */
class ATRDocIndex implements DocIndex {
	/** Map of documents to keys they have. */
	final HashMap<ATRDocument, HashSet<String>> documentToKeys = new HashMap<ATRDocument, HashSet<String>>();
	/** Map of documents to their version. */
	final HashMap<ATRDocument, Long> documentToVersion = new HashMap<ATRDocument, Long>();
	/**
	 * Map of cached values to pairs of Document/String Hashmaps for lookup and PrefixBTrees for
	 * fast indexing.
	 */
	final HashMap<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>> valueCache =
			new HashMap<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>>();
	final ATRStore s;
	boolean closed = false;
	ProgressMonitor pm;
	final StringPool stringPool;

	ATRDocIndex(ATRStore s, ProgressMonitor pm, StringPool stringPool) throws FatalStorageException {
		this.s = s;
		this.pm = pm;
		this.stringPool = stringPool;
		load();
	}

	public void setProgressMonitor(ProgressMonitor pm) { this.pm = pm; }

	/** Call this when you're done using the index. */
	public void close() {
		save();
		closed = true;
	}

	public SearchResult search(Map<String, String> searchKV, int maxDocs) {
		if (closed) { throw new RuntimeException("Key index has been closed."); }
		// Search keys
		List<ATRDocument> docs = new LinkedList<ATRDocument>(s.docs);
		for (String k : searchKV.keySet()) {
			if (valueCache.containsKey(k)) {
				docs.retainAll(valueCache.get(k).a.keySet());
			}
		}
		// Search values
		for (Entry<String, String> e : searchKV.entrySet()) {
			if (e.getValue() != null && e.getValue().length() > 0) {
				docs.retainAll(valueCache.get(e.getKey()).b.get(e.getValue()));
			}
		}
		// Determine co-keys
		List<String> coKeyList = new ArrayList<String>();
		if (docs.size() == s.idToDoc.size()) {
			coKeyList.addAll(valueCache.keySet());
		} else {
			int loop = 0;
			HashSet<String> coKeys = new HashSet<String>();
			for (ATRDocument d : docs) {
				HashSet<String> keys = documentToKeys.get(d);
				if (keys != null) { coKeys.addAll(keys); }
				loop++;
				if (loop % 1000 == 0) {
					if (coKeys.containsAll(valueCache.keySet())) {
						break;
					}
				}
			}
			coKeys.removeAll(searchKV.keySet());
			coKeyList.addAll(coKeys);
		}
		Collections.sort(coKeyList);
		int numDocs = docs.size();
		List<Document> docsToReturn = (List<Document>) (List)(
				(maxDocs == ALL_DOCS || docs.size() <= maxDocs)
				? docs
				: docs.subList(0, maxDocs));
		// Cast to List and then to List<Document> to force correct return type.
		SearchOutcome outcome = SearchOutcome.EXHAUSTIVE;
		if (maxDocs != ALL_DOCS && docs.size() > maxDocs) {
			outcome = SearchOutcome.DOC_LIMIT_REACHED;
		}
		return new SearchResult(docsToReturn, coKeyList, outcome, numDocs);
	}

	public boolean isKey(String key) { return valueCache.containsKey(key); }

	public List<String> allKeys() {
		ArrayList<String> keys = new ArrayList<String>(valueCache.keySet());
		Collections.sort(keys);
		return keys;
	}

	void documentDeleted(ATRDocument d) {
		documentToKeys.remove(d);
		documentToVersion.remove(d);
		for (Map.Entry<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>> e :
				valueCache.entrySet())
		{
			e.getValue().a.remove(d);
			e.getValue().b.remove(e.getKey(), d);
		}
	}

	/** Called when a key is added to a document. */
	void keyAdded(ATRDocument d, String key, String value) {
		// Put it into the index of documents to keys
		if (!documentToKeys.containsKey(d)) {
			documentToKeys.put(d, new HashSet<String>());
		}
		documentToKeys.get(d).add(key);
		// Now store it in the cache
		if (!valueCache.containsKey(key)) {
			valueCache.put(key, p(new HashMap<Document, String>(), new PrefixBTree<Document>()));
		}
		String oldValue = valueCache.get(key).a.get(d);
		valueCache.get(key).a.put(d, value);
		if (oldValue != null) {
			valueCache.get(key).b.remove(oldValue, d);
		}
		valueCache.get(key).b.put(value, d);
	}

	/** Called when a key is removed from a document. */
	void keyRemoved(ATRDocument d, String key) throws FatalStorageException {
		// Remove it from the index from documents to keys
		HashSet<String> keys = documentToKeys.get(d);
		if (keys == null) {
			reindex();
			return;
		}
		keys.remove(key);
		if (keys.isEmpty()) {
			documentToKeys.remove(d);
		}
		// Remove it from the value cache.
		String oldValue = valueCache.get(key).a.get(d);
		if (oldValue != null) {
			valueCache.get(key).a.remove(d);
			valueCache.get(key).b.remove(oldValue, d);
			if (valueCache.get(key).a.size() == 0) {
				valueCache.remove(key);
			}
		}
	}

	List<String> getKeys(ATRDocument d) {
		return documentToKeys.containsKey(d)
				? new ArrayList<String>(documentToKeys.get(d))
				: Collections.<String>emptyList();
	}

	boolean hasKey(ATRDocument d, String key) {
		return documentToKeys.containsKey(d)
				? documentToKeys.get(d).contains(key)
				: false;
	}

	String getCachedValue(ATRDocument d, String key) {
		return valueCache.containsKey(key)
				? valueCache.get(key).a.get(d)
				: null;
	}

	void nextVersion(ATRDocument d) {
		documentToVersion.put(d, getCachedVersion(d) + 1);
	}

	long getCachedVersion(ATRDocument d) {
		return documentToVersion.containsKey(d) ? documentToVersion.get(d) : 0;
	}

	/** Create fresh index from documents in store. */
	void reindex() throws FatalStorageException {
		pm.showProgressBar("Rebuilding index", "Starting...", s.docs.size());
		documentToKeys.clear();
		documentToVersion.clear();
		valueCache.clear();
		int loops = 0;
		for (ATRDocument d : s.docs) {
			if (loops++ % 10 == 0) {
				pm.progress(loops + " documents of " + s.docs.size(), loops);
			}
			for (Map.Entry<String, String> kv : d.data.kv().entrySet()) {
				if (kv.getKey().startsWith(ATRDocument.DATA_PREFIX)) {
					keyAdded(d, kv.getKey().substring(ATRDocument.DATA_PREFIX.length()),
							kv.getValue());
				}
			}
			documentToVersion.put(d, d.data.version());
		}
		pm.hideProgressBar();
	}

	/** Saves the index into an ATR file. */
	void save() {
		pm.showProgressBar("Saving index", "", valueCache.size());
		File f = new File(s.location, "index.atr");
		mkAncestors(f);
		ATRWriter w = null;
		try {
			w = new ATRWriter(new BufferedOutputStream(new FileOutputStream(f, /*append*/false)));
			int progress = 0;
			// First, just say how many keys there are for better progress metering.
			w.startRecord();
			w.write(string(valueCache.size() + 1));
			w.endRecord();
			// Save version info
			pm.progress("Saving version info", progress++);
			for (Entry<ATRDocument, Long> e : documentToVersion.entrySet()) {
				w.startRecord();
				w.write(string(e.getKey().getID()));
				w.write(string(e.getValue()));
				w.endRecord();
			}
			// End version info with empty record
			w.startRecord();
			w.endRecord();
			for (Entry<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>> e : valueCache.entrySet()) {
				pm.progress("Saving tag \"" + e.getKey() + "\"", progress++);

				// Write a record containing the key we're storing.
				w.startRecord();
				w.write(e.getKey());
				w.endRecord();

				// Now write a series of records for each document to value mapping for that key.
				for (Entry<Document, String> dAndValue : e.getValue().a.entrySet()) {
					w.startRecord();
					w.write(string(dAndValue.getKey().getID()));
					w.write(dAndValue.getValue());
					w.endRecord();
				}

				// End the key info with an empty record.
				w.startRecord();
				w.endRecord();
			}
			// End the whole file with another empty record.
			w.startRecord();
			w.endRecord();
			pm.progress("Finishing...", valueCache.size());
		} catch (Exception e) {
			throw new RuntimeException("Couldn't write index to " + f + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
		}
		pm.hideProgressBar();
	}

	/** Loads the index from an ATR file. */
	void load() throws FatalStorageException {
		pm.showProgressBar("Loading index", "", -1);
		documentToKeys.clear();
		documentToVersion.clear();
		valueCache.clear();
		File f = new File(s.location, "index.atr");
		if (f.exists()) {
			ATRReader r = null;
			try {
				r = new ATRReader(new BufferedInputStream(new FileInputStream(f)));
				String[] rec = new String[2];
				r.readRecord(rec, 0, 1);
				int numKeys = integer(rec[0]);
				pm.showProgressBar("Loading index", "", numKeys);
				// Load versions
				pm.progress("Loading version info", 0);
				while (r.readRecord(rec, 0, 2) > 0) {
					ATRDocument d = (ATRDocument) s.get(integer(rec[0]));
					if (d == null) {
						throw new RuntimeException("Document " + rec[0] + " doesn't exist.");
					}
					documentToVersion.put(d, Long.valueOf(rec[1]));
				}
				int keyNum = 1;
				while (r.readRecord(rec, 0, 1) > 0) {
					String key = stringPool.poolNoCutoff(rec[0]);
					pm.progress("Loading tag \"" + key + "\"", keyNum++);
					Pair<HashMap<Document, String>, PrefixBTree<Document>> cache =
							p(new HashMap<Document, String>(), new PrefixBTree<Document>());
					valueCache.put(key, cache);
					while (r.readRecord(rec, 0, 2) > 0) {
						ATRDocument d = (ATRDocument) s.get(integer(rec[0]));
						if (d == null) {
							throw new RuntimeException("Document " + rec[0] + " doesn't exist.");
						}
						String value = stringPool.pool(rec[1]);
						cache.a.put(d, value);
						cache.b.put(value, d);
						HashSet<String> docKeys;
						if (!documentToKeys.containsKey(d)) {
							docKeys = new HashSet<String>();
							documentToKeys.put(d, docKeys);
						} else {
							docKeys = documentToKeys.get(d);
						}
						docKeys.add(key);
					}
				}
			} catch (Exception e) {
				// Something went wrong, so we just ignore this index and reindex. In the future we
				// may want to log what went wrong, or tell the user why this is taking so long.
				reindex();
			} finally {
				try { r.close(); } catch (Exception e) {}
			}
			
			// Since we'd rather have to reindex than have an outdated index, delete the index file.
			f.delete();
		} else {
			// No index file, so index from scratch.
			reindex();
		}
		pm.hideProgressBar();
	}
}
