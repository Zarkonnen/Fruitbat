package com.metalbeetle.fruitbat.hierarchicalstorage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.fruitbat.io.DataSink.CommittableOutputStream;
import com.metalbeetle.fruitbat.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.SearchOutcome;
import com.metalbeetle.fruitbat.storage.SearchResult;
import com.metalbeetle.fruitbat.util.Pair;
import com.metalbeetle.fruitbat.util.PrefixBTree;
import com.metalbeetle.fruitbat.util.StringPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.LinkedList;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

/** Key index for HSStore. */
public class HSIndex implements DocIndex {
	/** Map of documents to keys they have. */
	final HashMap<HSDocument, HashSet<String>> documentToKeys = new HashMap<HSDocument, HashSet<String>>();
	/**
	 * Map of cached values to pairs of Document/String Hashmaps for lookup and PrefixBTrees for
	 * fast indexing.
	 */
	final HashMap<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>> valueCache =
			new HashMap<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>>();
	/** Map of Documents to their revision. */
	final HashMap<HSDocument, String> documentToRevision = new HashMap<HSDocument, String>();
	final HSStore s;
	boolean closed = false;
	ProgressMonitor pm;
	final StringPool stringPool;

	HSIndex(HSStore s, ProgressMonitor pm, StringPool stringPool) throws FatalStorageException {
		this.s = s;
		this.pm = pm;
		this.stringPool = stringPool;
		load();
	}

	public void setProgressMonitor(ProgressMonitor pm) { this.pm = pm; }

	/** Call this when you're done using the index. */
	public void close() {
		try {
			save();
		} catch (FatalStorageException e) {
			pm.handleException(new FatalStorageException("Unable to save index.", e), null);
		}
		closed = true;
	}

	public SearchResult search(Map<String, String> searchKV, int maxDocs) {
		if (closed) { throw new RuntimeException("Key index has been closed."); }
		// Search keys
		List<HSDocument> docs = new LinkedList<HSDocument>(s.docs);
		for (String k : searchKV.keySet()) {
			if (valueCache.containsKey(k)) {
				docs.retainAll(valueCache.get(k).a.keySet());
			} else {
				docs.clear();
			}
		}
		// Search values
		for (Entry<String, String> e : searchKV.entrySet()) {
			if (e.getValue() != null && e.getValue().length() > 0) {
				Pair<HashMap<Document, String>, PrefixBTree<Document>> cached = valueCache.get(e.getKey());
				if (cached != null) { docs.retainAll(cached.b.get(e.getValue())); }
			}
		}
		// Determine co-keys
		HashSet<String> coKeys = new HashSet<String>();
		if (docs.size() == s.idToDoc.size()) {
			coKeys.addAll(valueCache.keySet());
		} else {
			int loop = 0;
			for (HSDocument d : docs) {
				HashSet<String> keys = documentToKeys.get(d);
				if (keys != null) { coKeys.addAll(keys); }
				loop++;
				if (loop % 1000 == 0) {
					if (coKeys.containsAll(valueCache.keySet())) {
						break;
					}
				}
			}
		}
		coKeys.removeAll(searchKV.keySet());
		List<String> coKeyList = new ArrayList<String>(coKeys);
		Collections.sort(coKeyList);
		int numDocs = docs.size();
		// Cast to List and then to List<Document> to force correct return type.
		List<Document> docsToReturn = (List<Document>) (List)(
				(maxDocs == ALL_DOCS || docs.size() <= maxDocs)
				? docs
				: docs.subList(0, maxDocs));
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

	void removeDocument(HSDocument d) {
		documentToKeys.remove(d);
		for (Iterator<Map.Entry<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>>> it =
				valueCache.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry<String, Pair<HashMap<Document, String>, PrefixBTree<Document>>> e = it.next();
			e.getValue().a.remove(d);
			e.getValue().b.remove(e.getKey(), d);
			if (e.getValue().a.isEmpty()) {
				it.remove();
			}
		}
	}

	void addDocument(HSDocument d) throws FatalStorageException {
		for (String key : d.data.keys()) {
			if (key.startsWith(HSDocument.DATA_PREFIX)) {
				keyAdded(d, key.substring(HSDocument.DATA_PREFIX.length()), d.data.get(key));
			}
		}
	}

	/** Called when a key is added to a document. */
	void keyAdded(HSDocument d, String key, String value) throws FatalStorageException {
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
	void keyRemoved(HSDocument d, String key) throws FatalStorageException {
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
			if (valueCache.get(key).a.isEmpty()) {
				valueCache.remove(key);
			}
		}
	}

	/** Called when the revision of a document changes. */
	void revisionChanged(HSDocument d, String revision) {
		documentToRevision.put(d, revision);
	}

	String getCachedRevision(HSDocument d) {
		return documentToRevision.get(d);
	}

	List<String> getKeys(HSDocument d) {
		return documentToKeys.containsKey(d)
				? new ArrayList<String>(documentToKeys.get(d))
				: Collections.<String>emptyList();
	}

	boolean hasKey(HSDocument d, String key) {
		return documentToKeys.containsKey(d)
				? documentToKeys.get(d).contains(key)
				: false;
	}

	String getCachedValue(HSDocument d, String key) {
		return valueCache.containsKey(key)
				? valueCache.get(key).a.get(d)
				: null;
	}

	/** Create fresh index from documents in store. */
	void reindex() throws FatalStorageException {
		int numDocs = s.docs.size();
		pm.newProcess("Rebuilding index", "Starting...", numDocs);
		documentToKeys.clear();
		valueCache.clear();
		documentToRevision.clear();
		int loops = 0;
		for (HSDocument d : s.docs) {
			if (loops++ % 10 == 0) {
				pm.progress(loops + " documents of " + numDocs, loops);
			}
			documentToRevision.put(d, d.data.get(HSDocument.REVISION_KEY));
			for (String key : d.data.keys()) {
				if (key.startsWith(HSDocument.DATA_PREFIX)) {
					keyAdded(d, key.substring(HSDocument.DATA_PREFIX.length()),
							d.data.get(key));
				}
			}
		}
	}

	/** Saves the index into an ATR file. */
	void save() throws FatalStorageException {
		pm.newProcess("Saving index", "", valueCache.size());
		Location l = s.location.child("index.atr");
		ATRWriter w = null;
		CommittableOutputStream tos = null;
		try {
			tos = l.getOutputStream();
			w = new ATRWriter(new BufferedOutputStream(tos.stream()));
			int progress = 0;
			// First, store the revision info:
			for (Entry<HSDocument, String> e : documentToRevision.entrySet()) {
				w.startRecord();
				w.write(string(e.getKey().getID()));
				w.write(e.getValue());
				w.endRecord();
			}
			// End the revision info with an empty record.
			w.startRecord();
			w.endRecord();

			// Second, store the key/value cache:
			// First, just say how many keys there are for better progress metering.
			w.startRecord();
			w.write(string(valueCache.size() + 1));
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
			w.flush();
		} catch (Exception e) {
			// We couldn't write the index, so let's just try to delete it.
			if (tos.isAbortable()) {
				tos.abort();
			}
			l.delete();
			throw new FatalStorageException("Couldn't write index to " + l.getName() + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
			try {
				tos.commitIfNotAborted();
			} catch (IOException e) {
				throw new FatalStorageException("Couldn't write index to " + l.getName() + ".", e);
			}
		}
	}

	/** Loads the index from an ATR file. */
	void load() throws FatalStorageException {
		pm.newProcess("Loading index", "", -1);
		documentToKeys.clear();
		valueCache.clear();
		documentToRevision.clear();
		Location l = s.location.child("index.atr");
		if (l.exists()) {
			ATRReader r = null;
			try {
				r = new ATRReader(new BufferedInputStream(l.getInputStream()));
				String[] rec = new String[2];
				// Load the revision cache:
				while (r.readRecord(rec, 0, 2) > 0) {
					HSDocument d = (HSDocument) s.get(integer(rec[0]));
					if (d == null) {
						throw new RuntimeException("Document " + rec[0] + " doesn't " +
								"exist.");
					}
					documentToRevision.put(d, rec[1]);
				}

				// Load the k/v cache:
				r.readRecord(rec, 0, 1);
				int numKeys = integer(rec[0]);
				pm.changeNumSteps(numKeys);
				int keyNum = 1;
				while (r.readRecord(rec, 0, 1) > 0) {
					String key = stringPool.poolNoCutoff(rec[0]);
					pm.progress("Loading tag \"" + key + "\"", keyNum++);
					Pair<HashMap<Document, String>, PrefixBTree<Document>> cache =
							p(new HashMap<Document, String>(), new PrefixBTree<Document>());
					valueCache.put(key, cache);
					while (r.readRecord(rec, 0, 2) > 0) {
						HSDocument d = (HSDocument) s.get(integer(rec[0]));
						if (d == null) {
							throw new RuntimeException("Document " + rec[0] + " doesn't " +
									"exist.");
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
				// Something went wrong, so we just ignore this index and reindex. In the future
				// we may want to log what went wrong, or tell the user why this is taking so long.
				reindex();
			} finally {
				try { r.close(); } catch (Exception e) {}
			}

			// Since we'd rather have to reindex than have an outdated index, delete the index.
			l.delete();
		} else {
			// No index file, so index from scratch.
			reindex();
		}
	}
}
