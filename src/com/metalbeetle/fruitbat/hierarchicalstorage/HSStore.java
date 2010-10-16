package com.metalbeetle.fruitbat.hierarchicalstorage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.util.StringPool;
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

/** Stores documents on some hierarchical substrate. */
public abstract class HSStore implements Store {
	static final String METADATA_PREFIX = "md ";
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

	protected final Location location;
	protected final Location docsL;
	protected final KVFile metaF;
	protected final HashMap<Integer, HSDocument> idToDoc = new HashMap<Integer, HSDocument>();
	protected final List<HSDocument> docs = new LinkedList<HSDocument>();
	protected final HSIndex index;
	protected boolean revisionUpdated = false;

	public HSStore(Location location, ProgressMonitor pm) throws FatalStorageException {
		pm.showProgressBar("Loading documents", "", -1);
		try {
			this.location = location;
			docsL = location.child("docs");
			HashMap<String, String> myMetaDefaults = new HashMap<String, String>(META_DEFAULTS);
			myMetaDefaults.put(STORE_UUID, UUID.randomUUID().toString());
			metaF = location.child("meta.atr").kvFile(location.child("meta-cache.atr"), myMetaDefaults);
			if (docsL.exists()) {
				List<Location> ls = docsL.children();
				ArrayList<Location> filteredLs = new ArrayList<Location>();
				for (Location l : ls) {
					if (l.getName().matches("[0-9]+")) {
						filteredLs.add(l);
					}
				}
				pm.changeNumSteps(filteredLs.size());
				int loop = 0;
				for (Location f : filteredLs) {
					HSDocument d = new HSDocument(f, this);
					idToDoc.put(d.getID(), d);
					docs.add(d);
					if (loop++ % 100 == 0) {
						pm.progress(docs.size() + " documents loaded", loop);
					}
				}
				pm.progress("Sorting documents", -1);
				Collections.sort(docs);
				Collections.reverse(docs);
			}
			index = new HSIndex(this, pm, new StringPool(4));
			pm.progress("Loading full text index", -1);
			metaF.saveToCache();
            revisionUpdated = false;
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
		if (getFullTextIndex() != null) {
			getFullTextIndex().close();
		}
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

	public Location getLocation() { return location; }

	public List<Document> docs() { return (List<Document>) (List) immute(docs); }

	public Document get(int id) { return idToDoc.get(id); }

	public Document create() throws FatalStorageException {
		return create(metaF.get(NEXT_DOC_ID));
	}

	Document create(String id) throws FatalStorageException {
		try {
			HSDocument d = new HSDocument(docsL.child(id), this);
			if (integer(id) >= integer(metaF.get(NEXT_DOC_ID))) {
				metaF.change(l(DataChange.put(NEXT_DOC_ID, string(integer(id) + 1))));
			}
			idToDoc.put(d.getID(), d);
			docs.add(0, d);
			d.change(/*revision*/ "0", l(
					DataChange.put(Fruitbat.CREATION_DATE_KEY, currentDateString()),
					DataChange.put(Fruitbat.ALIVE_KEY, "")));
			updateRevision();
			return d;
		} catch (Exception e) {
			throw new FatalStorageException("Unable to create new document in " + location + ".",
					e);
		}
	}

	public Document getOrCreate(int id) throws FatalStorageException {
		if (idToDoc.containsKey(id)) { return idToDoc.get(id); }
		return create(string(id));
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
