package com.metalbeetle.fruitbat.gui;

import javax.swing.JLabel;
import javax.swing.JPanel;

class PagesViewer extends JPanel {
	final DocumentFrame df;

	PagesViewer(DocumentFrame df) {
		this.df = df;
		add(new JLabel("[view of document's pages]"));
	}
	
}
