package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.HashSet;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

class SearchColorizingDocument extends DefaultStyledDocument {
	final Style tagStyle;
	final Style unknownTagStyle;
	final Style valueStyle;
	final Style ignoredTagStyle;
	final MainFrame mf;
	final HashSet<String> encounteredKeys = new HashSet<String>();

	SearchColorizingDocument(MainFrame mf) {
		this.mf = mf;
		tagStyle = mf.searchF.addStyle("Tag", null);
		StyleConstants.setForeground(tagStyle, Colors.MATCHED_TAG);
		unknownTagStyle = mf.searchF.addStyle("Unknown Tag", null);
		StyleConstants.setForeground(unknownTagStyle, Colors.UNKNOWN_TAG);
		valueStyle = mf.searchF.addStyle("Value", null);
		StyleConstants.setForeground(valueStyle, Colors.VALUE);
		ignoredTagStyle = mf.searchF.addStyle("Ignored Tag", null);
		StyleConstants.setForeground(ignoredTagStyle, Colors.IGNORED_TAG);
	}

	@Override
	public void remove(int offs, int len) throws BadLocationException {
		super.remove(offs, len);
		colorize();
	}

	@Override
	public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
		super.insertString(offset, str.replaceAll("[\\n\\r]", ""), a);
		colorize();
	}

	void colorize() {
		try {
			int start = 0;
			int nextSpace;
			int nextColon;
			final String text = mf.searchF.getText();
			encounteredKeys.clear();
			do {
				nextSpace = text.indexOf(" ", start);
				if (nextSpace == -1) { nextSpace = text.length(); }
				nextColon = text.indexOf(":", start);
				if (nextColon == -1) { nextColon = text.length() + 1; }
				if (nextColon < nextSpace) {
					String key = text.substring(start, nextColon);
					if (encounteredKeys.contains(key)) {
						setCharacterAttributes(start, nextSpace, ignoredTagStyle, true);
					} else {
						encounteredKeys.add(key);
						setCharacterAttributes(start, nextColon,
								mf.getIndex().isKey(key) ? tagStyle : unknownTagStyle, true);
						setCharacterAttributes(nextColon, nextSpace, valueStyle, true);
					}
				} else {
					String key = text.substring(start, nextSpace);
					if (encounteredKeys.contains(key)) {
						setCharacterAttributes(start, nextSpace, ignoredTagStyle, true);
					} else {
						encounteredKeys.add(key);
						setCharacterAttributes(start, nextSpace,
								mf.getIndex().isKey(key) ? tagStyle : unknownTagStyle, true);
					}
				}
				start = nextSpace + 1;
			} while (nextSpace != text.length());
		} catch (FatalStorageException e) {
			mf.handleException(e);
		}
	}
}
