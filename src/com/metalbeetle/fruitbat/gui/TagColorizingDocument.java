package com.metalbeetle.fruitbat.gui;

import java.util.HashSet;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

public class TagColorizingDocument extends DefaultStyledDocument {
	final Style tagStyle;
	final Style valueStyle;
	final Style ignoredTagStyle;
	final JTextPane tagF;
	final HashSet<String> encounteredKeys = new HashSet<String>();

	TagColorizingDocument(JTextPane tagF) {
		this.tagF = tagF;
		tagStyle = tagF.addStyle("Tag", null);
		StyleConstants.setForeground(tagStyle, Colors.TAG);
		valueStyle = tagF.addStyle("Value", null);
		StyleConstants.setForeground(valueStyle, Colors.VALUE);
		ignoredTagStyle = tagF.addStyle("Ignored Tag", null);
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
		int start = 0;
		int nextSpace;
		int nextColon;
		final String text = tagF.getText();
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
					setCharacterAttributes(start, nextColon, tagStyle, true);
					setCharacterAttributes(nextColon, nextSpace, valueStyle, true);
				}
			} else {
				String key = text.substring(start, nextSpace);
				if (encounteredKeys.contains(key)) {
					setCharacterAttributes(start, nextSpace, ignoredTagStyle, true);
				} else {
					encounteredKeys.add(key);
					setCharacterAttributes(start, nextSpace, tagStyle, true);
				}
			}
			start = nextSpace + 1;
		} while (nextSpace != text.length());
	}
}
