package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

class SearchColorizingDocument extends DefaultStyledDocument {
	final Style tagStyle;
	final Style unknownTagStyle;
	final Style valueStyle;
	final Style noopStyle;
	final JTextPane searchF;
	final Fruitbat app;

	SearchColorizingDocument(JTextPane searchF, Fruitbat app) {
		this.app = app;
		this.searchF = searchF;
		tagStyle = searchF.addStyle("Tag", null);
		StyleConstants.setForeground(tagStyle, Colors.MATCHED_TAG);
		unknownTagStyle = searchF.addStyle("Unknown Tag", null);
		StyleConstants.setForeground(unknownTagStyle, Colors.UNKNOWN_TAG);
		valueStyle = searchF.addStyle("Value", null);
		StyleConstants.setForeground(valueStyle, Colors.VALUE);
		noopStyle = searchF.addStyle("Noop", null);
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
		final String text = searchF.getText();
		do {
			nextSpace = text.indexOf(" ", start);
			if (nextSpace == -1) { nextSpace = text.length(); }
			nextColon = text.indexOf(":", start);
			if (nextColon == -1) { nextColon = text.length() + 1; }
			if (nextColon < nextSpace) {
				String key = text.substring(start, nextColon);
				setCharacterAttributes(start, nextColon,
						app.getIndex().isKey(key) ? tagStyle : unknownTagStyle, true);
				setCharacterAttributes(nextColon, nextSpace, valueStyle, true);
			} else {
				String key = text.substring(start, nextSpace);
				setCharacterAttributes(start, nextSpace,
						app.getIndex().isKey(key) ? tagStyle : unknownTagStyle, true);
			}
			start = nextSpace + 1;
		} while (nextSpace != text.length());
		
		setCharacterAttributes(0, text.length(), noopStyle, false);
	}
}
