package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.HashSet;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

class SearchColorizingDocument extends DefaultStyledDocument implements UndoableEditListener {
	final Style tagStyle;
	final Style unknownTagStyle;
	final Style valueStyle;
	final Style ignoredTagStyle;
	final Style fullTextStyle;
	final Style disabledFullTextStyle;
	final StoreFrame mf;
	final HashSet<String> encounteredKeys = new HashSet<String>();
	final HashSet<UndoableEditListener> uels = new HashSet<UndoableEditListener>();
	boolean undosEnabled = true;
	boolean undosEnabled2 = true;

	SearchColorizingDocument(StoreFrame mf) {
		this.mf = mf;
		tagStyle = mf.searchF.addStyle("Tag", null);
		StyleConstants.setForeground(tagStyle, Colors.MATCHED_TAG);
		unknownTagStyle = mf.searchF.addStyle("Unknown Tag", null);
		StyleConstants.setForeground(unknownTagStyle, Colors.UNKNOWN_TAG);
		valueStyle = mf.searchF.addStyle("Value", null);
		StyleConstants.setForeground(valueStyle, Colors.VALUE);
		ignoredTagStyle = mf.searchF.addStyle("Ignored Tag", null);
		StyleConstants.setForeground(ignoredTagStyle, Colors.IGNORED_TAG);
		fullTextStyle = mf.searchF.addStyle("Full Text", null);
		StyleConstants.setForeground(fullTextStyle, Colors.FULL_TEXT);
		StyleConstants.setItalic(fullTextStyle, true);
		disabledFullTextStyle = mf.searchF.addStyle("Full Text", null);
		StyleConstants.setForeground(disabledFullTextStyle, Colors.FULL_TEXT);
		StyleConstants.setItalic(disabledFullTextStyle, true);
		StyleConstants.setStrikeThrough(disabledFullTextStyle, true);
		super.addUndoableEditListener(this);
	}

	public void setUndosEnabled(boolean undosEnabled) {
		this.undosEnabled = undosEnabled;
	}

	@Override
	public void addUndoableEditListener(UndoableEditListener uel) {
		uels.add(uel);
	}

	@Override
	public void removeUndoableEditListener(UndoableEditListener uel) {
		uels.remove(uel);
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
			undosEnabled2 = false;
			int start = 0;
			int nextSpace;
			int nextColon;
			int nextQuote;
			final String text = mf.searchF.getText();
			encounteredKeys.clear();
			do {
				nextQuote = text.indexOf("\"", start);
				if (nextQuote == -1) { nextQuote = text.length() + 1; }
				nextSpace = text.indexOf(" ", start);
				if (nextSpace == -1) { nextSpace = text.length(); }
				nextColon = text.indexOf(":", start);
				if (nextColon == -1) { nextColon = text.length() + 1; }
				if (nextQuote < nextColon && nextQuote < nextSpace) {
					int quoteAfterThat =
							nextQuote == text.length() - 1
							? nextQuote
							: text.indexOf("\"", nextQuote + 1);
					if (quoteAfterThat == -1) { quoteAfterThat = text.length(); }
					setCharacterAttributes(start, nextColon + 1,
							mf.store.getFullTextIndex() == null
									? disabledFullTextStyle
									: fullTextStyle,
							true);
					if (quoteAfterThat >= text.length() - 1) {
						return;
					} else {
						start = quoteAfterThat + 1;
						continue;
					}
				}
				if (nextColon < nextSpace) {
					if (nextSpace > nextQuote) {
						nextSpace = nextQuote;
					}
					String key = text.substring(start, nextColon);
					if (encounteredKeys.contains(key)) {
						setCharacterAttributes(start, nextSpace, ignoredTagStyle, true);
					} else {
						encounteredKeys.add(key);
						setCharacterAttributes(start, nextColon,
								mf.isKey(key) ? tagStyle : unknownTagStyle, true);
						setCharacterAttributes(nextColon, nextSpace, valueStyle, true);
					}
				} else {
					String key = text.substring(start, nextSpace);
					if (encounteredKeys.contains(key)) {
						setCharacterAttributes(start, nextSpace, ignoredTagStyle, true);
					} else {
						encounteredKeys.add(key);
						setCharacterAttributes(start, nextSpace,
								mf.isKey(key) ? tagStyle : unknownTagStyle, true);
					}
				}
				start = nextSpace + 1;
			} while (nextSpace != text.length());
		} catch (FatalStorageException e) {
			mf.handleException(e);
		} finally {
			undosEnabled2 = true;
		}
	}

	public void undoableEditHappened(UndoableEditEvent uee) {
		if (undosEnabled && undosEnabled2) {
			for (UndoableEditListener uel : uels) { uel.undoableEditHappened(uee); }
		}
	}
}
