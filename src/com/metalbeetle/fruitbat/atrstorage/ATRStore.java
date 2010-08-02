package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.util.StringPool;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

/** Stores documents on file system, using ATR files, which guarantees atomicity. */
class ATRStore implements Store {
	static final String _DELETED = "_deleted";
	static final String NEXT_DOC_ID = "next_doc_id";
	static final String NEXT_RETAINED_PAGE_NUMBER = "next_retained_page_id";
	static final Map<String, String> META_DEFAULTS = m(p(NEXT_DOC_ID, "0"),
			p(NEXT_RETAINED_PAGE_NUMBER, "1"));

	final File location;
	final File docsF;
	final KVFile metaF;
	final HashMap<Integer, ATRDocument> idToDoc = new HashMap<Integer, ATRDocument>();
	final List<ATRDocument> docs = new LinkedList<ATRDocument>();
	final ATRDocIndex index;

	public ATRStore(File location, ProgressMonitor pm) throws FatalStorageException {
		try {
			this.location = location;
			docsF = new File(location, "docs");
			metaF = new KVFile(new File(location, "meta.atr"), new File(location, "meta-cache.atr"),
					META_DEFAULTS);
			if (docsF.exists()) {
				File[] fs = docsF.listFiles(new DocFilter());
				pm.showProgressBar("Loading documents", "", fs.length);
				int loop = 0;
				for (File f : fs) {
					ATRDocument d = new ATRDocument(f, this);
					idToDoc.put(d.getID(), d);
					docs.add(d);
					if (loop++ % 100 == 0) {
						pm.progress(docs.size() + " documents loaded", loop);
					}
				}
				pm.progress("Sorting documents", -1);
				Collections.sort(docs);
				Collections.reverse(docs);
				//pm.hideProgressBar();
			}
			index = new ATRDocIndex(this, pm, new StringPool(4));
			metaF.saveToCache();
		} catch (Exception e) {
			throw new FatalStorageException("Could not start up document store at " + location +
					".", e);
		}
	}

	public DocIndex getIndex() { return index; }

	public void close() throws FatalStorageException {
		metaF.saveToCache();
		index.close();
	}

	public void setProgressMonitor(ProgressMonitor pm) {
		index.setProgressMonitor(pm);
	}

	public int getNextRetainedPageNumber() throws FatalStorageException {
		try {
			return integer(metaF.get(NEXT_RETAINED_PAGE_NUMBER));
		} catch (Exception e) {
			throw new FatalStorageException("Could not determine next retained page number.", e);
		}
	}

	public void setNextRetainedPageNumber(int n) throws FatalStorageException {
		try {
			metaF.change(l(DataChange.put(NEXT_RETAINED_PAGE_NUMBER, string(n))));
		} catch (Exception e) {
			throw new FatalStorageException("Could not set next retained page number.", e);
		}
	}

	static final class DocFilter implements FilenameFilter {
		public boolean accept(File dir, String name) { return name.matches("[0-9]+"); }
	}

	public File getLocation() { return location; }
	
	public List<Document> docs() { return (List<Document>) (List) immute(docs); }

	public Document get(int id) { return idToDoc.get(id); }
	
	public Document create() throws FatalStorageException {
		try {
			ATRDocument d = new ATRDocument(new File(docsF, metaF.get(NEXT_DOC_ID)), this);
			metaF.change(l(DataChange.put(NEXT_DOC_ID, string(integer(metaF.get(NEXT_DOC_ID)) + 1))));
			mkDirs(d.location);
			idToDoc.put(d.getID(), d);
			docs.add(0, d);
			d.change(l(DataChange.put(Fruitbat.CREATION_DATE_KEY, currentDateString())));
			return d;
		} catch (Exception e) {
			throw new FatalStorageException("Unable to create new document in " + location + ".",
					e);
		}
	}

	public void delete(Document d) throws FatalStorageException {
		ATRDocument d2 = (ATRDocument) d;
		File delF = new File(docsF, d2.location.getName() + _DELETED);
		if (!d2.location.renameTo(delF)) {
			throw new FatalStorageException("Couldn't delete document.\nUnable to move " +
					d2.location + " to " + delF + ".");
		}
		idToDoc.remove(d2.getID());
		docs.remove(d2);
		index.documentDeleted(d2);
	}

	public Document undelete(int docID) {
		File delF = new File(docsF, docID + _DELETED);
		File f = new File(docsF, string(docID));
		if (!delF.renameTo(f)) {
			throw new RuntimeException("Couldn't undelete document.\nUnable to move " + f + " to " +
					delF + ".");
		}
		ATRDocument d = new ATRDocument(f, this);
		idToDoc.put(d.getID(), d);
		return d;
	}
	
	@Override
	public String toString() {
		return "store@" + location;
	}
}
