package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import static com.metalbeetle.fruitbat.util.Misc.*;

class OpenDocManager {
	static final int DOC_WIN_OFFSET = 10;
	final HashMap<Document, DocumentFrame> openFrames = new HashMap<Document, DocumentFrame>();
	final HashSet<Listener> listeners = new HashSet<Listener>();
	final StoreFrame mf;

	static interface Listener {
		void documentOpened(StoreFrame mf, DocumentFrame df);
		void documentClosed(StoreFrame mf, DocumentFrame df);
	}

	public void addListener(Listener l) { listeners.add(l); }
	public void removeListener(Listener l) { listeners.remove(l); }

	public OpenDocManager(StoreFrame mf) {
		this.mf = mf;
	}

	DocumentFrame open(Document d) {
		if (!openFrames.containsKey(d)) {
			DocumentFrame df = new DocumentFrame(d, mf);
			df.setLocationRelativeTo(null);
			df.setVisible(true);
			openFrames.put(d, df);
			for (Listener l : listeners) {
				l.documentOpened(mf, df);
			}
		}
		DocumentFrame df = openFrames.get(d);
		df.toFront();
		return df;
	}

	DocumentFrame getAndToFrontIfOpen(Document d) {
		if (openFrames.containsKey(d)) {
			DocumentFrame df = openFrames.get(d);
			df.toFront();
			return df;
		}
		return null;
	}

	void close(Document d) {
		for (Listener l : listeners) {
			l.documentClosed(mf, openFrames.get(d));
		}
		openFrames.get(d).dispose();
		openFrames.remove(d);
	}

	void close() {
		for (Document d : openFrames.keySet()) {
			close(d);
		}
	}

	void writePrefs(Preferences node) throws BackingStoreException, FatalStorageException {
		node.clear();
		for (DocumentFrame df : openFrames.values()) {
			df.writePrefs(node.node(string(df.d.getID())));
		}
	}

	void readPrefs(Preferences node) throws BackingStoreException, FatalStorageException {
		DocumentFrame toFocus = null;
		for (String ids : node.childrenNames()) {
			int id = integer(ids);
			Document d = mf.store.get(id);
			if (d != null) {
				DocumentFrame df = open(d);
				df.readPrefs(node.node(ids));
				if (node.node(ids).getBoolean("focused", false)) {
					toFocus = df;
				}
			}
		}
		if (toFocus != null) {
			toFocus.toFront();
		}
	}
}
