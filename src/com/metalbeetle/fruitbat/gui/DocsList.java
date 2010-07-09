package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
				mf.quickLook(getSelectedIndex());
			}
			@Override
			public void focusLost(FocusEvent e) {
				mf.quickLook(null);
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				mf.quickLook(null);
			}
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					Document d = (Document) m.getElementAt(locationToIndex(e.getPoint()));
					mf.openDocManager.open(d, mf);
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int index = locationToIndex(e.getPoint());
				// Unfortunately, if the list is not full, hovering over the empty
				// bit of the bottom causes locationToIndex to claim you're still
				// hovering over the final item on it. So we fix this here.
				if (!getUI().getCellBounds(self, index, index).
						contains(e.getPoint()))
				{
					index = -1;
				}
				mf.quickLook(index);
			}
		});
		addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (hasFocus()) {
					mf.quickLook(getSelectedIndex());
				}
			}
		});
	}
}
