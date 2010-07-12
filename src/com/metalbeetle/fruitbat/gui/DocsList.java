package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JList;

/** List of documents in MainFrame. */
class DocsList extends JList {
	final MainFrame mf;
	final DocsListModel m;

	DocsList(MainFrame mf) {
		this(mf, new DocsListModel(mf));
	}

	private DocsList(final MainFrame mf, final DocsListModel m) {
		super(m);
		this.m = m;
		this.mf = mf;
		final DocsList self = this;

		setCellRenderer(new DocCellRenderer(mf));
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER && getSelectedIndex() != -1) {
					Document d = (Document) m.getElementAt(getSelectedIndex());
					mf.openDocManager.open(d, mf);
				}
			}
		});
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (getSelectedIndex() == -1 && m.getSize() > 0) {
					setSelectedIndex(0);
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					Document d = (Document) m.getElementAt(locationToIndex(e.getPoint()));
					mf.openDocManager.open(d, mf);
				}
			}
		});
	}
}
