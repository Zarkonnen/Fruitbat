package com.metalbeetle.fruitbat.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JList;
import javax.swing.text.BadLocationException;

/** List of suitable tags for narrowing search. */
class NarrowSearchTagsList extends JList {
	final MainFrame mf;
	final TagsListModel m;

	NarrowSearchTagsList(MainFrame mf) {
		this(mf, new TagsListModel(mf));
	}

	private NarrowSearchTagsList(final MainFrame mf, final TagsListModel m) {
		super(m);
		this.m = m;
		this.mf = mf;
		setCellRenderer(new TagCellRenderer(mf));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tagClick(locationToIndex(e.getPoint()));
			}
		});
		setFocusable(false);
	}

	void tagClick(int index) {
		if (index != -1) {
			String clicked = (String) m.getElementAt(index);
			String[] tags = mf.searchF.getText().split(" +");
			boolean tagAlreadyExists = false;
			if (clicked.contains(":")) {
				for (String t: tags) {
					if (clicked.equals(t)) { tagAlreadyExists = true; break; }
				}
			} else {
				for (String t : tags) {
					String key = t.split(":", 2)[0];
					if (key.equals(clicked)) { tagAlreadyExists = true; break; }
				}
			}
			if (tagAlreadyExists) {
				// Remove the tag.
				int searchStart = 0;
				int found = -1;
				String text = mf.searchF.getText();
				while ((found = text.indexOf(clicked, searchStart)) != -1) {
					// Check this is not just a substring of a larger tag.
					if (found != 0 && text.charAt(found - 1) != ' ') { continue; }
					int consumeToRight = 0;
					if (found + clicked.length() < text.length()) {
						char nextC = text.charAt(found + clicked.length());
						if (nextC == ' ') {
							consumeToRight = 1;
						} else {
							if (nextC == ':') {
								// The tag is a key/value pair. We want to get rid of both, so let's
								// see how far the value extends.
								int nextSpaceIndex = text.indexOf(" ", found + 1);
								if (nextSpaceIndex == -1) { nextSpaceIndex = text.length(); }
								consumeToRight = nextSpaceIndex - found - clicked.length();
							} else {
								// The tag we've clicked on happens to be the start of a preexisting
								// tag.
								continue;
							}
						}
					}
					// OK, it's bounded on both sides by the end of the document or spaces.
					try {
						mf.searchF.getDocument().remove(found, clicked.length() + consumeToRight);
						mf.search();
					} catch (BadLocationException e) {
						// La la la should not happen.
					}
					searchStart = found + 1;
				}
			} else {
				// See if we can use this tag to narrow a search.
				if (clicked.contains(":")) {
					String[] kv = clicked.split(":");
					int clickedIndex = mf.searchF.getText().indexOf(kv[0] + ":");
					if (clickedIndex != -1) {
						int spaceIndex = mf.searchF.getText().indexOf(" ", clickedIndex);
						if (spaceIndex == -1) { spaceIndex = mf.searchF.getText().length(); }
						try {
							mf.searchF.getDocument().remove(clickedIndex, spaceIndex - clickedIndex);
							mf.searchF.getDocument().insertString(clickedIndex, clicked, null);
							mf.search();
						} catch (BadLocationException e) {
							// La la la should not happen.
						}
						return;
					}
				}

				// Insert the tag.
				try {
					// Put spaces around the tag as needed.
					if (mf.searchF.getCaretPosition() != 0) {
						if (mf.searchF.getText().charAt(mf.searchF.getCaretPosition() - 1) != ' ') {
							clicked = " " + clicked;
						}
					}
					mf.searchF.getDocument().insertString(mf.searchF.getCaretPosition(), clicked,
							null);
					mf.search();
				} catch (BadLocationException e) {
					// La la la should not happen.
				}
			}
		}
	}
}
