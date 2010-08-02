package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import java.util.HashMap;

public class OpenDocManager {
	static final int DOC_WIN_OFFSET = 10;
	final HashMap<Document, DocumentFrame> openFrames = new HashMap<Document, DocumentFrame>();

	void open(Document d, MainFrame mf) {
		if (!openFrames.containsKey(d)) {
			DocumentFrame df = new DocumentFrame(d, mf);
			df.setLocation(mf.getLocation().x + DOC_WIN_OFFSET,
					mf.getLocation().y + DOC_WIN_OFFSET);
			df.setVisible(true);
			openFrames.put(d, df);
		}
		openFrames.get(d).toFront();
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
}
