package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.RevisionNotFoundException;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.util.StringPool;
import java.io.File;
import java.io.FilenameFilter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

/** Stores documents on file system, using ATR files, which guarantees atomicity. */
class ATRStore implements Store {
	static final String METADATA_PREFIX = "md ";
	static final String _DELETED = "_deleted";
	static final String NEXT_DOC_ID = "next_doc_id";
	static final String NEXT_RETAINED_PAGE_NUMBER = "next_retained_page_id";
	static final String EMPTY_STORE = "empty_store";
	static final String REVISION = "revision";
	static final String STORE_UUID = "uuid";
	static final Map<String, String> META_DEFAULTS = m(
			p(NEXT_DOC_ID, "0"),
			p(NEXT_RETAINED_PAGE_NUMBER, "1"),
			p(EMPTY_STORE, "true"),
			p(REVISION, "0"));

	final File location;
	final File docsF;
	final KVFile metaF;
	final HashMap<Integer, ATRDocument> idToDoc = new HashMap<Integer, ATRDocument>();
	final List<ATRDocument> docs = new LinkedList<ATRDocument>();
	final ATRDocIndex index;
	final HashMap<Integer, ATRDocument> idToDeletedDoc = new HashMap<Integer, ATRDocument>();
	final List<ATRDocument> deletedDocs = new LinkedList<ATRDocument>();
	final ATRDocIndex deletedIndex;
	boolean revisionUpdated = false;

	public ATRStore(File location, ProgressMonitor pm) throws FatalStorageException {
		pm.showProgressBar("Loading documents", "", -1);
		try {
			this.location = location;
			docsF = new File(location, "docs");
			HashMap<String, String> myMetaDefaults = new HashMap<String, String>(META_DEFAULTS);
			myMetaDefaults.put(STORE_UUID, UUID.randomUUID().toString());
			metaF = new KVFile(new File(location, "meta.atr"), new File(location, "meta-cache.atr"),
					myMetaDefaults);
			if (docsF.exists()) {
				File[] fs = docsF.listFiles(new DocFilter());
				pm.changeNumSteps(fs.length);
				int loop = 0;
				for (File f : fs) {
					ATRDocument d = new ATRDocument(f, this, /*deleted*/ false);
					idToDoc.put(d.getID(), d);
					docs.add(d);
					if (loop++ % 100 == 0) {
						pm.progress(docs.size() + " documents loaded", loop);
					}
				}
				pm.progress("Sorting documents", -1);
				Collections.sort(docs);
				Collections.reverse(docs);

				fs = docsF.listFiles(new DeletedDocFilter());
				pm.showProgressBar("Checking for deleted documents", "", fs.length);
				try {
					loop = 0;
					for (File f : fs) {
						ATRDocument d = new ATRDocument(f, this, /*deleted*/ true);
						idToDeletedDoc.put(d.getID(), d);
						deletedDocs.add(d);
						if (loop++ % 100 == 0) {
							pm.progress(deletedDocs.size() + " deleted documents found", loop);
						}
					}
					pm.progress("Sorting deleted documents", -1);
					Collections.sort(deletedDocs);
					Collections.reverse(deletedDocs);
				} finally {
					pm.hideProgressBar();
				}
			}
			index = new ATRDocIndex(this, pm, new StringPool(4), /*forDeleteds*/ false);
			deletedIndex = new ATRDocIndex(this, pm, new StringPool(4), /*forDeleteds*/ true);
			metaF.saveToCache();
		} catch (Exception e) {
			throw new FatalStorageException("Could not start up document store at " + location +
					".", e);
		} finally {
			pm.hideProgressBar();
		}
	}

	public DocIndex getIndex() { return index; }

	public void close() throws FatalStorageException {
		metaF.saveToCache();
		index.close();
		deletedIndex.close();
	}

	public void setProgressMonitor(ProgressMonitor pm) {
		index.setProgressMonitor(pm);
		deletedIndex.setProgressMonitor(pm);
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

	public List<Document> deletedDocs() { return (List<Document>) (List) immute(deletedDocs); }

	public String getMetaData(String key) throws FatalStorageException {
		try {
			return metaF.get(METADATA_PREFIX + key);
		} catch (FatalStorageException e) {
			throw new FatalStorageException("Metadata key " + key + " not found.", e);
		}
	}

	public boolean hasMetaData(String key) throws FatalStorageException {
		return metaF.has(METADATA_PREFIX + key);
	}

	public void changeMetaData(List<Change> changes) throws FatalStorageException {
		ArrayList<Change> mdc = new ArrayList<Change>();
		for (Change c : changes) {
			if (c instanceof DataChange.Put) {
				DataChange.Put p = (DataChange.Put) c;
				mdc.add(DataChange.put(METADATA_PREFIX + p.key, p.value));
				continue;
			}
			if (c instanceof DataChange.Remove) {
				DataChange.Remove r = (DataChange.Remove) c;
				mdc.add(DataChange.remove(METADATA_PREFIX + r.key));
				continue;
			}
			if (c instanceof DataChange.Move) {
				DataChange.Move m = (DataChange.Move) c;
				mdc.add(DataChange.move(METADATA_PREFIX + m.srcKey, METADATA_PREFIX + m.dstKey));
				continue;
			}
		}
		mdc.addAll(revisionChanges());
		metaF.change(mdc);
	}

	public List<String> metaDataKeys() throws FatalStorageException {
		ArrayList<String> keys = new ArrayList<String>();
		for (String k : metaF.keys()) {
			if (k.startsWith(METADATA_PREFIX)) {
				keys.add(k.substring(METADATA_PREFIX.length()));
			}
		}
		return keys;
	}

	public String getUUID() throws FatalStorageException {
		try {
			return metaF.get(STORE_UUID);
		} catch (Exception e) {
			throw new FatalStorageException("Could not get store unique identifier.", e);
		}
	}

	public String getRevision() throws FatalStorageException {
		try {
			return metaF.get(REVISION);
		} catch (Exception e) {
			throw new FatalStorageException("Could not get store revision.", e);
		}
	}

	public boolean isEmptyStore() throws FatalStorageException {
		try {
			return metaF.get(EMPTY_STORE).equals("true");
		} catch (Exception e) {
			throw new FatalStorageException("Could not determine if store is empty.", e);
		}
	}

	public List<Change> getChangesSince(String revision) throws FatalStorageException, RevisionNotFoundException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String revert(String toRevision) throws FatalStorageException, RevisionNotFoundException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	static final class DocFilter implements FilenameFilter {
		public boolean accept(File dir, String name) { return name.matches("[0-9]+"); }
	}

	static final class DeletedDocFilter implements FilenameFilter {
		public boolean accept(File dir, String name) { return name.matches("[0-9]+" + _DELETED); }
	}

	public File getLocation() { return location; }
	
	public List<Document> docs() { return (List<Document>) (List) immute(docs); }

	public Document get(int id) { return idToDoc.get(id); }
	public Document getDeleted(int id) { return idToDeletedDoc.get(id); }

	public Document create() throws FatalStorageException {
		return create(metaF.get(NEXT_DOC_ID));
	}

	Document create(String id) throws FatalStorageException {
		try {
			ATRDocument d = new ATRDocument(new File(docsF, id), this, /*deleted*/ false);
			if (integer(id) >= integer(metaF.get(NEXT_DOC_ID))) {
				metaF.change(l(DataChange.put(NEXT_DOC_ID, string(integer(id) + 1))));
			}			
			mkDirs(d.location);
			idToDoc.put(d.getID(), d);
			docs.add(0, d);
			d.change(/*revision*/ "0", l(
					DataChange.put(Fruitbat.CREATION_DATE_KEY, currentDateString())));
			updateRevision();
			return d;
		} catch (Exception e) {
			throw new FatalStorageException("Unable to create new document in " + location + ".",
					e);
		}
	}

	public Document getCreateOrUndelete(int id) throws FatalStorageException {
		if (idToDoc.containsKey(id)) { return idToDoc.get(id); }
		if (idToDeletedDoc.containsKey(id)) { return undelete(id); }
		return create(string(id));
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
		index.removeDocument(d2);
		ATRDocument delD = new ATRDocument(delF, this, /*deleted*/ true);
		idToDeletedDoc.put(delD.getID(), delD);
		deletedDocs.add(delD);
		deletedIndex.addDocument(delD);
	}

	public Document undelete(int docID) throws FatalStorageException {
		ATRDocument deletedDoc = idToDeletedDoc.get(docID);
		File delF = new File(docsF, docID + _DELETED);
		File f = new File(docsF, string(docID));
		if (!delF.renameTo(f)) {
			throw new RuntimeException("Couldn't undelete document.\nUnable to move " + f + " to " +
					delF + ".");
		}
		ATRDocument d = new ATRDocument(f, this, /*deleted*/ false);
		idToDoc.put(d.getID(), d);
		docs.add(d);
		index.addDocument(d);
		deletedDocs.remove(idToDeletedDoc.get(docID));
		idToDeletedDoc.remove(docID);
		deletedIndex.removeDocument(deletedDoc);
		return d;
	}

	void updateRevision() throws FatalStorageException {
		metaF.change(revisionChanges());
	}

	List<Change> revisionChanges() throws FatalStorageException {
		if (!revisionUpdated) {
			revisionUpdated = true;
			BigInteger rev = new BigInteger(metaF.get(REVISION), 16);
			BigInteger newRev = rev.add(BigInteger.ONE);
			if (metaF.get(EMPTY_STORE).equals("true")) {
				return l(
						DataChange.put(EMPTY_STORE, "false"),
						DataChange.put(REVISION, newRev.toString(16))
				);
			} else {
				return l(
						DataChange.put(REVISION, newRev.toString(16))
				);
			}
		}
		return Collections.emptyList();
	}
	
	@Override
	public String toString() {
		return "store@" + location;
	}
}
