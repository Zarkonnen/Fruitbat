package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.fruitbat.gui.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.SearchOutcome;
import com.metalbeetle.fruitbat.storage.SearchResult;
import com.metalbeetle.fruitbat.util.Pair;
import com.metalbeetle.fruitbat.util.PrefixBTree;
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
import java.util.Iterator;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

/** Key index for ATRStore. */
public class ATRDocIndex implements DocIndex {
	static final List<String> CACHED_KEYS = l("d");
	/** Map of keys to documents they're in. */
	final HashMap<String, HashSet<ATRDocument>> keyToDocuments = new HashMap<String, HashSet<ATRDocument>>();
	/** Map of documents to keys they have. */
	final HashMap<ATRDocument, HashSet<String>> documentToKeys = new HashMap<ATRDocument, HashSet<String>>();
	/**
	 * Map of cached values to pairs of Document/String Hashmaps for lookup and PrefixBTrees for
	 * fast indexing.
	 */
	final HashMap<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>> valueCache =
			new HashMap<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>>();
	final ATRStore s;
	boolean closed = false;
	ProgressMonitor pm;

	public ATRDocIndex(ATRStore s, ProgressMonitor pm) {
		this.s = s;
		this.pm = pm;
		s.setKeyIndex(this);
		load();
	}

	void resetValueCache() {
		valueCache.clear();
		for (String k : CACHED_KEYS) {
			valueCache.put(k, p(new HashMap<Document, String>(), new PrefixBTree<Document>()));
		}
	}

	/** Call this when you're done using the index. */
	public void close() {
		save();
		closed = true;
	}

	public SearchResult search(Map<String, String> searchKV, int maxDocs, int timeoutMs) {
		if (closed) { throw new RuntimeException("Key index has been closed."); }
		// Search keys
		List<ATRDocument> docs = new LinkedList<ATRDocument>(s.idToDoc.values());
		for (String k : searchKV.keySet()) {
			if (keyToDocuments.containsKey(k)) {
				docs.retainAll(keyToDocuments.get(k));
			}
		}
		// Search values
		for (Iterator<ATRDocument> it = docs.iterator(); it.hasNext();) {
			ATRDocument d = it.next();
			for (Entry<String, String> e : searchKV.entrySet()) {
				if (e.getValue() != null &&
					e.getValue().length() > 0 &&
				    hasKey(d, e.getKey()) &&
				    !d.get(e.getKey()).startsWith(e.getValue()))
				{
					it.remove();
					break;
				}
			}
		}
		// Determine co-keys
		List<String> coKeyList = new ArrayList<String>();
		if (docs.size() == s.idToDoc.size()) {
			coKeyList.addAll(keyToDocuments.keySet());
		} else {
			int loops = 0;
			HashSet<String> coKeys = new HashSet<String>();
			for (ATRDocument d : docs) {
				coKeys.addAll(documentToKeys.get(d));
				loops++;
				if (loops % 1000 == 0) {
					if (coKeys.containsAll(keyToDocuments.keySet())) {
						break;
					}
				}
			}
			coKeys.removeAll(searchKV.keySet());
			coKeyList.addAll(coKeys);
		}
		Collections.sort(coKeyList);
		Collections.sort(docs);
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

	public boolean isKey(String key) { return keyToDocuments.containsKey(key); }

	/** Called when a key is added to a document. */
	void keyAdded(ATRDocument d, String key, String value) {
		// Put it into the index of keys to documents.
		if (!keyToDocuments.containsKey(key)) {
			keyToDocuments.put(key, new HashSet<ATRDocument>());
		}
		keyToDocuments.get(key).add(d);
		// Put it into the index of documents to keys
		if (!documentToKeys.containsKey(d)) {
			documentToKeys.put(d, new HashSet<String>());
		}
		documentToKeys.get(d).add(key);
		// If its value is being cached by the index, store it in the cache
		if (CACHED_KEYS.contains(key)) {
			String oldValue = valueCache.get(key).a.get(d);
			valueCache.get(key).a.put(d, value);
			if (oldValue != null) {
				valueCache.get(key).b.remove(oldValue, d);
			}
			valueCache.get(key).b.put(value, d);
		}
	}

	/** Called when a key is removed from a document. */
	void keyRemoved(ATRDocument d, String key) {
		// Remove it from the index from keys to documents.
		HashSet<ATRDocument> ds = keyToDocuments.get(key);
		if (ds == null) {
			// Something's wrong, so just reindex.
			reindex();
			return;
		}
		ds.remove(d);
		if (ds.isEmpty()) {
			keyToDocuments.remove(key);
		}
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
		// Remove it from the value cache if needed.
		if (CACHED_KEYS.contains(key)) {
			String oldValue = valueCache.get(key).a.get(d);
			if (oldValue != null) {
				valueCache.get(key).a.remove(d);
				valueCache.get(key).b.remove(oldValue, d);
			}
		}
	}

	List<String> getKeys(ATRDocument d) {
		return new ArrayList<String>(documentToKeys.get(d));
	}

	boolean hasKey(ATRDocument d, String key) {
		return documentToKeys.get(d).contains(key);
	}

	String getCachedValue(ATRDocument d, String key) {
		if (CACHED_KEYS.contains(key)) {
			return valueCache.get(key).a.get(d);
		}
		return null;
	}

	/** Create fresh index from documents in store. */
	void reindex() {
		pm.showProgressBar("Rebuilding index", "Starting...", s.idToDoc.size());
		keyToDocuments.clear();
		documentToKeys.clear();
		resetValueCache();
		int loops = 0;
		for (ATRDocument d : s.idToDoc.values()) {
			if (loops++ % 100 == 0) {
				pm.progress(loops + " documents of " + s.idToDoc.size(), loops);
			}
			for (Map.Entry<String, String> kv : d.data.kv().entrySet()) {
				keyAdded(d, kv.getKey(), kv.getValue());
			}
		}
		pm.hideProgressBar();
	}

	/** Saves the index into an ATR file. */
	void save() {
		pm.showProgressBar("Saving index", "", -1);
		File f = new File(s.location, "keyindex.atr");
		mkAncestors(f);
		ATRWriter w = null;
		try {
			w = new ATRWriter(new BufferedOutputStream(new FileOutputStream(f, /*append*/false)));
			// Each line is a key followed by the IDs of the documents that have that key.
			for (Entry<String, HashSet<ATRDocument>> e : keyToDocuments.entrySet()) {
				w.startRecord();
				w.write(e.getKey());
				for (ATRDocument d : e.getValue()) {
					w.write(d.getID());
				}
				w.endRecord();
			}
			// We put a single empty record at the end to indicate we managed to write the
			// whole index.
			w.startRecord();
			w.endRecord();
			// Now we write any value caches we may have.
			for (Map.Entry<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>> e :
				valueCache.entrySet())
			{
				// Indicate which key this cache is for.
				w.startRecord();
				w.write(e.getKey());
				w.endRecord();
				// Now store it as tuples of document ID and value.
				for (Map.Entry<Document, String> tp : e.getValue().a.entrySet()) {
					w.startRecord();
					w.write(tp.getKey().getID());
					w.write(tp.getValue());
					w.endRecord();
				}
				// Indicate this value cache is complete.
				w.startRecord();
				w.endRecord();
			}
			// Now write another empty record to indicate we're done with the file.
			w.startRecord();
			w.endRecord();
		} catch (Exception e) {
			throw new RuntimeException("Couldn't write index to " + f + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
		}
		pm.hideProgressBar();
	}

	/** Loads the index from an ATR file. */
	void load() {
		pm.showProgressBar("Loading index", "", -1);
		resetValueCache();
		File f = new File(s.location, "keyindex.atr");
		if (f.exists()) {
			ATRReader r = null;
			try {
				r = new ATRReader(new BufferedInputStream(new FileInputStream(f)));
				List<String> rec;
				while (!(rec = r.readRecord()).isEmpty()) {
					String key = rec.get(0);
					HashSet<ATRDocument> docs = new HashSet<ATRDocument>(rec.size() * 2);
					for (String id : rec.subList(1, rec.size())) {
						ATRDocument d = (ATRDocument) s.get(id);
						if (d == null) {
							throw new RuntimeException("Document " + id + " doesn't exist.");
						}
						docs.add(d);
						if (!documentToKeys.containsKey(d)) {
							documentToKeys.put(d, new HashSet<String>());
						}
						documentToKeys.get(d).add(key);
					}
					keyToDocuments.put(key, docs);
				}
				// Now we load the value caches.
				while (!(rec = r.readRecord()).isEmpty()) {
					Pair<HashMap<Document, String>, PrefixBTree<Document>> c =
							valueCache.get(rec.get(0));
					if (c == null) {
						throw new RuntimeException(rec.get(0) + " is not a cached key.");
					}
					while (!(rec = r.readRecord()).isEmpty()) {
						ATRDocument d = (ATRDocument) s.get(rec.get(0));
						if (d == null) {
							throw new RuntimeException("Document " + rec.get(0) + " doesn't " +
									"exist.");
						}
						c.a.put(d, rec.get(1));
						c.b.put(rec.get(1), d);
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
