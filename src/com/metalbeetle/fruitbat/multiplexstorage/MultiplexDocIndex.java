package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.SearchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class MultiplexDocIndex implements DocIndex {
	final MultiplexStore s;

	MultiplexDocIndex(MultiplexStore s) {
		this.s = s;
	}

	public SearchResult search(Map<String, String> searchKV, int maxDocs) throws FatalStorageException {
		SearchResult sr = null;
		while (true) {
			try {
				sr = s.fastest().getIndex().search(searchKV, maxDocs);
				break;
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}

		// Now construct a SearchResult which references 
		ArrayList<Document> docs = new ArrayList<Document>(sr.docs.size());
		for (Document d : sr.docs) {
			docs.add(s.get(d.getID()));
		}

		return new SearchResult(Collections.unmodifiableList(docs), sr.narrowingTags,
				sr.outcome, sr.minimumAvailableDocs);
	}

	public boolean isKey(String key) throws FatalStorageException {
		while (true) {
			try {
				return s.fastest().getIndex().isKey(key);
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public List<String> allKeys() throws FatalStorageException {
		while (true) {
			try {
				return s.fastest().getIndex().allKeys();
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public void close() {
		for (EnhancedStore es : s.stores) {
			try {
				es.getIndex().close();
			} catch (Exception e) {
				// Ignore.
			}
		}
	}
}
