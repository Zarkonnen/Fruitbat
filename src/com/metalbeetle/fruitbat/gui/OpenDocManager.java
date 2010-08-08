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

	void open(Document d) {
		if (!openFrames.containsKey(d)) {
			DocumentFrame df = new DocumentFrame(d, mf);
			df.setLocation(mf.getLocation().x + DOC_WIN_OFFSET,
					mf.getLocation().y + DOC_WIN_OFFSET);
			df.setVisible(true);
			openFrames.put(d, df);
		}
		openFrames.get(d).toFront();
	}

	DocumentFrame open(Document d, int x, int y, int w, int h) {
		if (!openFrames.containsKey(d)) {
			DocumentFrame df = new DocumentFrame(d, mf);
			df.setLocation(x, y);
			df.setSize(w, h);
			df.setVisible(true);
			openFrames.put(d, df);
		}
		return openFrames.get(d);
	}

	void close(Document d) {
		openFrames.get(d).dispose();
		openFrames.remove(d);
	}

	void close() {
		for (DocumentFrame df : openFrames.values()) {
			df.saveTags();
		}
	}

	void writePrefs(Preferences node) throws BackingStoreException, FatalStorageException {
		node.clear();
		for (DocumentFrame df : openFrames.values()) {
			Preferences dfn = node.node(string(df.d.getID()));
			dfn.putInt("x", df.getX());
			dfn.putInt("y", df.getY());
			dfn.putInt("width", df.getWidth());
			dfn.putInt("height", df.getHeight());
			dfn.putBoolean("focused", df.isFocused());
		}
	}

	void readPrefs(Preferences node) throws BackingStoreException, FatalStorageException {
		DocumentFrame toFocus = null;
		for (String ids : node.childrenNames()) {
			int id = integer(ids);
			Document d = mf.store.get(id);
			if (d != null) {
				Preferences dfn = node.node(ids);
				int x = dfn.getInt("x", mf.getLocation().x + DOC_WIN_OFFSET);
				int y = dfn.getInt("y", mf.getLocation().y + DOC_WIN_OFFSET);
				int w = dfn.getInt("width", DocumentFrame.WIDTH);
				int h = dfn.getInt("height", DocumentFrame.HEIGHT);
				DocumentFrame df = open(d, x, y, w, h);
				if (dfn.getBoolean("focused", false)) {
					toFocus = df;
				}
			}
		}
		if (toFocus != null) {
			toFocus.toFront();
		}
	}
}
