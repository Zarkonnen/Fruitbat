package com.metalbeetle.fruitbat.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import javax.swing.JList;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;

/** List of tags which, when clicked, are added/removed to/from a JTextPane. */
abstract class TagsList extends JList {
	TagsList() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tagClick(locationToIndex(e.getPoint()));
			}
		});
		setFocusable(false);
	}

	abstract JTextPane getTagsField();
	abstract void postChange();
	HashSet<String> getUsedKeys() {
		HashSet<String> keys = new HashSet<String>();
		String[] tags = getTagsField().getText().split(" +");
		for (String t : tags) {
			if (t.length() > 0) { keys.add(t.split(":", 2)[0]); }
		}
		return keys;
	}

	void tagClick(int index) {
		if (!isEnabled()) { return; }
		if (index != -1) {
			String clicked = (String) getModel().getElementAt(index);
			String[] tags = getTagsField().getText().split(" +");
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
				String text = getTagsField().getText();
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
						getTagsField().getDocument().remove(found, clicked.length() + consumeToRight);
					} catch (BadLocationException e) {
						// La la la should not happen.
					}
					searchStart = found + 1;
				}
				postChange();
			} else {
				// See if we can use this tag to narrow a search.
				if (clicked.contains(":")) {
					String[] kv = clicked.split(":");
					int clickedIndex = getTagsField().getText().indexOf(kv[0] + ":");
					if (clickedIndex != -1) {
						int spaceIndex = getTagsField().getText().indexOf(" ", clickedIndex);
						if (spaceIndex == -1) { spaceIndex = getTagsField().getText().length(); }
						try {
							getTagsField().getDocument().remove(clickedIndex, spaceIndex - clickedIndex);
							getTagsField().getDocument().insertString(clickedIndex, clicked, null);
							postChange();
						} catch (BadLocationException e) {
							// La la la should not happen.
						}
						return;
					}
				}

				// Insert the tag.
				try {
					// Put spaces around the tag as needed.
					if (getTagsField().getCaretPosition() != 0) {
						if (getTagsField().getText().charAt(getTagsField().getCaretPosition() - 1)
								!= ' ')
						{
							clicked = " " + clicked;
						}
					}
					getTagsField().getDocument().insertString(getTagsField().getCaretPosition(),
							clicked, null);
					postChange();
				} catch (BadLocationException e) {
					// La la la should not happen.
				}
			}
		}
	}
}
