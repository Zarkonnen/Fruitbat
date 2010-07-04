package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.Store;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

/** Stores documents on file system, using ATR files, which guarantees atomicity. */
public class ATRStore implements Store {
	static final String _DELETED = "_deleted";
	static final String NEXT_DOC_ID = "next_doc_id";
	static final Map<String, String> META_DEFAULTS = m(p(NEXT_DOC_ID, "0"));

	final File location;
	final File docsF;
	final KVFile metaF;
	final HashMap<String, ATRDocument> idToDoc = new HashMap<String, ATRDocument>();
	ATRDocIndex keyIndex;

	public ATRStore(File location) {
		this.location = location;
		docsF = new File(location, "docs");
		metaF = new KVFile(new File(location, "meta.atr"), META_DEFAULTS);
		if (docsF.exists()) {
			for (File f : docsF.listFiles(new DocFilter())) {
				ATRDocument d = new ATRDocument(f, this);
				idToDoc.put(d.getID(), d);
			}
		}
	}

	static final class DocFilter implements FilenameFilter {
		public boolean accept(File dir, String name) { return name.matches("[0-9]+"); }
	}

	public File getLocation() { return location; }
	
	public List<Document> docs() { return (List<Document>) (List) immute(idToDoc.values()); }

	public Document get(String id) { return idToDoc.get(id); }
	
	public Document create() {
		try {
			ATRDocument d = new ATRDocument(new File(docsF, metaF.get(NEXT_DOC_ID)), this);
			metaF.put(NEXT_DOC_ID, string(integer(metaF.get(NEXT_DOC_ID)) + 1));
			mkDirs(d.location);
			idToDoc.put(d.getID(), d);
			return d;
		} catch (Exception e) {
			throw new RuntimeException("Unable to create new document in " + location + ".", e);
		}
	}

	public void delete(Document d) {
		ATRDocument d2 = (ATRDocument) d;
		File delF = new File(docsF, d2.location.getName() + _DELETED);
		if (!d2.location.renameTo(delF)) {
			throw new RuntimeException("Couldn't delete document.\nUnable to move " + d2.location +
					" to " + delF + ".");
		}
		idToDoc.remove(d2.getID());
	}

	public Document undelete(String docID) {
		File delF = new File(docsF, docID + _DELETED);
		File f = new File(docsF, docID);
		if (!delF.renameTo(f)) {
			throw new RuntimeException("Couldn't undelete document.\nUnable to move " + f + " to " +
					delF + ".");
		}
		ATRDocument d = new ATRDocument(f, this);
		idToDoc.put(d.getID(), d);
		return d;
	}

	// Used to manage key indexes.
	void setKeyIndex(ATRDocIndex keyIndex) {
		if (this.keyIndex != null) { this.keyIndex.close(); }
		this.keyIndex = keyIndex;
	}

	void keyAdded(ATRDocument d, String key) {
		if (keyIndex != null) { keyIndex.keyAdded(d, key); }
	}

	void keyRemoved(ATRDocument d, String key) {
		if (keyIndex != null) { keyIndex.keyRemoved(d, key); }
	}

	@Override
	public String toString() {
		return "store@" + location;
	}
}
