package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.fulltext.FullTextIndex;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MultiplexFullTextIndex implements FullTextIndex {
	final MultiplexStore s;

	public MultiplexFullTextIndex(MultiplexStore s) {
		this.s = s;
	}

	public List<Document> query(List<List<String>> phrases, List<Document> within) throws FatalStorageException {
		while (true) {
			EnhancedStore fastest = s.fastestWithFullText();
			if (fastest == null) {
				return within; // No fulltext search, ignore.
			}
			try {
				ArrayList<Document> myWithin = new ArrayList<Document>(within);
				for (Document d : within) {
					myWithin.add(fastest.get(d.getID()));
				}
				List<Document> ds = s.fastest().getFullTextIndex().query(phrases, myWithin);
				ArrayList<Document> multiplexDs = new ArrayList<Document>(ds.size());
				for (Document d : ds) {
					multiplexDs.add(s.get(d.getID()));
				}
				return Collections.unmodifiableList(multiplexDs);
			} catch (FatalStorageException e) {
				s.handleStorageException(s.fastest(), e);
			}
		}
	}

	public void close() {
		for (EnhancedStore ms : s.stores) {
			if (ms.getFullTextIndex() != null) {
				ms.getFullTextIndex().close();
			}
		}
	}

	public void pageAdded(DataSrc text, Document doc) throws FatalStorageException {
		// Handled in substores.
	}

	public void pageRemoved(DataSrc text, Document doc) throws FatalStorageException {
		// Handled in substores.
	}
}
