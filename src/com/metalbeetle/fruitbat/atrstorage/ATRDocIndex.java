package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.util.Pair;
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
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

/** Key index for ATRStore. */
public class ATRDocIndex implements DocIndex {
	static final String EOF = "EOF";
	/** Map of keys to documents they're in. */
	final HashMap<String, HashSet<ATRDocument>> index = new HashMap<String, HashSet<ATRDocument>>();
	final ATRStore s;
	boolean closed = false;

	public ATRDocIndex(ATRStore s) {
		this.s = s;
		s.setKeyIndex(this);
		load();
	}

	/** Call this when you're done using the index. */
	public void close() {
		save();
		closed = true;
	}

	public Pair<List<Document>, List<String>> search(Map<String, String> searchKV) {
		if (closed) { throw new RuntimeException("Key index has been closed."); }
		// Search keys
		List<ATRDocument> docs = new LinkedList<ATRDocument>(s.idToDoc.values());
		for (String k : searchKV.keySet()) {
			if (index.containsKey(k)) {
				docs.retainAll(index.get(k));
			}
		}
		// Search values
		for (Iterator<ATRDocument> it = docs.iterator(); it.hasNext();) {
			ATRDocument d = it.next();
			for (Entry<String, String> e : searchKV.entrySet()) {
				if (e.getValue() != null &&
				    d.has(e.getKey())    &&
				    !d.get(e.getKey()).startsWith(e.getValue()))
				{
					it.remove();
					break;
				}
			}
		}
		// Determine co-keys
		HashSet<String> coKeys = new HashSet<String>();
		for (ATRDocument d : docs) {
			coKeys.addAll(d.data.kv().keySet()); // Violating encapsulation within package for speed
		}
		coKeys.removeAll(searchKV.keySet());
		List<String> coKeyList = new ArrayList<String>(coKeys);
		Collections.sort(coKeyList);
		Collections.sort(docs);
		// Cast to List and then to List<Document> to force correct return type.
		return p((List<Document>) (List) docs, coKeyList);
	}

	public boolean isKey(String key) { return index.containsKey(key); }

	/** Called when a key is added to a document. */
	void keyAdded(ATRDocument d, String key) {
		if (!index.containsKey(key)) {
			index.put(key, new HashSet<ATRDocument>());
		}
		index.get(key).add(d);
	}

	/** Called when a key is removed from a document. */
	void keyRemoved(ATRDocument d, String key) {
		HashSet<ATRDocument> ds = index.get(key);
		if (ds == null) {
			// Something's wrong, so just reindex.
			reindex();
		} else {
			ds.remove(d);
			if (ds.isEmpty()) {
				index.remove(key);
			}
		}
	}

	/** Create fresh index from documents in store. */
	void reindex() {
		index.clear();
		for (ATRDocument d : s.idToDoc.values()) {
			for (String key : d.data.kv().keySet()) {
				keyAdded(d, key);
			}
		}
	}

	/** Saves the index into an ATR file. */
	void save() {
		File f = new File(s.location, "keyindex.atr");
		mkAncestors(f);
		ATRWriter w = null;
		try {
			w = new ATRWriter(new BufferedOutputStream(new FileOutputStream(f, /*append*/false)));
			// Each line is a key followed by the IDs of the documents that have that key.
			for (Entry<String, HashSet<ATRDocument>> e : index.entrySet()) {
				w.startRecord();
				w.write(e.getKey());
				for (ATRDocument d : e.getValue()) {
					w.write(d.getID());
				}
				w.endRecord();
			}
			// Finally, we put a single empty record at the end to indicate we managed to write the
			// whole file.
			w.startRecord();
			w.endRecord();
		} catch (Exception e) {
			throw new RuntimeException("Couldn't write key index to " + f + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
		}
	}

	/** Loads the index from an ATR file. */
	void load() {
		File f = new File(s.location, "keyindex.atr");
		if (f.exists()) {
			ATRReader r = null;
			try {
				r = new ATRReader(new BufferedInputStream(new FileInputStream(f)));
				List<String> rec;
				while (!(rec = r.readRecord()).isEmpty()) {
					HashSet<ATRDocument> docs = new HashSet<ATRDocument>(rec.size() * 2);
					for (String id : rec.subList(1, rec.size())) {
						ATRDocument d = (ATRDocument) s.get(id);
						if (d == null) {
							throw new RuntimeException("Document " + id + " doesn't exist.");
						}
						docs.add(d);
					}
					index.put(rec.get(0), docs);
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
	}
}
