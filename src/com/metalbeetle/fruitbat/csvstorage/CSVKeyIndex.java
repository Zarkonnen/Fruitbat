package com.metalbeetle.fruitbat.csvstorage;

import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.KeyIndex;
import com.metalbeetle.fruitbat.util.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.LinkedList;
import java.util.List;
import com.mindprod.csv.CSVWriter;
import com.mindprod.csv.CSVReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

public class CSVKeyIndex implements KeyIndex {
	static final String EOF = "EOF";
	/** Map of keys to documents they're in. */
	final HashMap<String, HashSet<CSVDocument>> index = new HashMap<String, HashSet<CSVDocument>>();
	final CSVStore s;
	boolean closed = false;

	public CSVKeyIndex(CSVStore s) {
		this.s = s;
		s.setKeyIndex(this);
		load();
	}

	/** Call this when you're done using the index. */
	public void close() {
		save();
		closed = true;
	}

	public Pair<List<Document>, List<String>> searchKeys(List<String> searchKeys) {
		if (closed) { throw new RuntimeException("Key index has been closed."); }
		List<CSVDocument> docs = new LinkedList<CSVDocument>(s.idToDoc.values());
		for (String t : searchKeys) {
			if (index.containsKey(t)) {
				docs.retainAll(index.get(t));
			}
		}
		HashSet<String> coKeys = new HashSet<String>();
		for (CSVDocument d : docs) {
			coKeys.addAll(d.data.kv().keySet()); // Violating encapsulation within package for speed
		}
		coKeys.removeAll(searchKeys);
		List<String> coKeyList = new ArrayList<String>(coKeys);
		Collections.sort(coKeyList);
		Collections.sort(docs);
		// Cast to List and then to List<Document> to force correct return type.
		return p((List<Document>) (List) docs, coKeyList);
	}

	/** Called when a key is added to a document. */
	void keyAdded(CSVDocument d, String key) {
		if (!index.containsKey(key)) {
			index.put(key, new HashSet<CSVDocument>());
		}
		index.get(key).add(d);
	}

	/** Called when a key is removed from a document. */
	void keyRemoved(CSVDocument d, String key) {
		HashSet<CSVDocument> ds = index.get(key);
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

	void reindex() {
		index.clear();
		for (CSVDocument d : s.idToDoc.values()) {
			for (String key : d.data.kv().keySet()) {
				keyAdded(d, key);
			}
		}
	}

	/** Saves the index into a CSV file. */
	void save() {
		File f = new File(s.location + "keyindex.csv");
		mkAncestors(f);
		CSVWriter w = null;
		try {
			w = new CSVWriter(
					new OutputStreamWriter(
						new FileOutputStream(f, /*append*/ false),
					"UTF-8"),
					/*quoteLevel*/ 2, /*separator*/ ',', /*quote*/ '"', /*trim*/ true);
			// Each line is a key followed by the IDs of the documents that have that key.
			for (Entry<String, HashSet<CSVDocument>> e : index.entrySet()) {
				w.put(e.getKey());
				for (CSVDocument d : e.getValue()) {
					w.put(d.id);
				}
				w.nl();
			}
			// Finally, we put a single EOF line at the end to indicate we managed to write the
			// whole file.
			w.put(EOF);
		} catch (Exception e) {
			throw new RuntimeException("Couldn't write key index to " + f + ".", e);
		} finally {
			try { w.close(); } catch (Exception e) {}
		}
	}

	/** Loads the index from a CSV file. */
	void load() {
		File f = new File(s.location + "keyindex.csv");
		if (f.exists()) {
			CSVReader r = null;
			try {
				r = new CSVReader(
						new BufferedReader(
							new InputStreamReader(
								new FileInputStream(f),
								"UTF-8")
						),
						/*separator*/ ',', /*quote*/ '"', /*multiline*/ false, /*trim*/ true);
				String key;
				while (!(key = r.get()).equals(EOF)) {
					String[] docIDs = r.getAllFieldsInLine();
					HashSet<CSVDocument> docs = new HashSet<CSVDocument>(docIDs.length * 2);
					for (String id : docIDs) {
						CSVDocument d = s.get(id);
						if (d == null) {
							throw new RuntimeException("Document " + id + " doesn't exist.");
						}
						docs.add(d);
					}
					index.put(key, docs);
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
