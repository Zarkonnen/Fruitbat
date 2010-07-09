package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import java.util.HashMap;
import javax.swing.JFrame;

public class OpenDocManager {
	final HashMap<Document, DocumentFrame> openFrames = new HashMap<Document, DocumentFrame>();

	void open(Document d, JFrame parent) {
		if (!openFrames.containsKey(d)) {
			DocumentFrame df = new DocumentFrame(d, this);
			df.setLocation(parent.getLocation().x + 50, parent.getLocation().y + 50);
			df.setVisible(true);
			openFrames.put(d, df);
		}
		openFrames.get(d).toFront();
	}

	void close(Document d) {
		openFrames.get(d).dispose();
		openFrames.remove(d);
	}
}
