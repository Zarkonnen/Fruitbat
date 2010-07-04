package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

class ColorizingDocument extends DefaultStyledDocument {

	final Style tagStyle;
	final Style unknownTagStyle;
	final JTextPane searchF;
	final Fruitbat app;

	ColorizingDocument(JTextPane searchF, Fruitbat app) {
		this.app = app;
		this.searchF = searchF;
		tagStyle = searchF.addStyle("Tag", null);
		StyleConstants.setForeground(tagStyle, Colors.MATCHED_TAG);
		unknownTagStyle = searchF.addStyle("Unknown Tag", null);
		StyleConstants.setForeground(unknownTagStyle, Colors.UNKNOWN_TAG);
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
		int end;
		String text = searchF.getText();
		do {
			end = text.indexOf(" ", start);
			if (end == -1) {
				end = text.length();
			}
			String key = text.substring(start, end);
			setCharacterAttributes(start, end, app.getIndex().isKey(key) ? tagStyle : unknownTagStyle, true);
			start = end + 1;
		} while (end != text.length());
	}
}
