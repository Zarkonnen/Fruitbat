package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import static com.metalbeetle.fruitbat.util.Misc.*;

class OpenDocManager {
	static final int DOC_WIN_OFFSET = 10;
	final HashMap<Document, DocumentFrame> openFrames = new HashMap<Document, DocumentFrame>();
	final MainFrame mf;

	public OpenDocManager(MainFrame mf) {
		this.mf = mf;
	}

	DocumentFrame open(Document d) {
		if (!openFrames.containsKey(d)) {
			DocumentFrame df = new DocumentFrame(d, mf);
			df.setLocation(mf.getLocation().x + DOC_WIN_OFFSET,
					mf.getLocation().y + DOC_WIN_OFFSET);
			df.setVisible(true);
			openFrames.put(d, df);
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
