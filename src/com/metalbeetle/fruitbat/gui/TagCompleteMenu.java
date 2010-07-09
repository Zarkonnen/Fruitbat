package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.gui.Colors.*;

/** Popup menu for offering possible completions of a tag. */
public class TagCompleteMenu extends JPopupMenu {
	static final int MAX_SUGGESTED_VALUES = 30;

	public TagCompleteMenu(final MainFrame mf) {
		final String text = mf.searchF.getText();
		final int caretPos = mf.searchF.getCaretPosition();
		final int spacePos = Math.max(0, text.substring(0, caretPos).lastIndexOf(" ") + 1);
		final int colonPos = Math.max(0, text.substring(0, caretPos).lastIndexOf(":") + 1);
		if (spacePos >= colonPos) {
			// Suggest a tag
			final String tagFragment = text.substring(spacePos, caretPos);
			for (final String tag : join(mf.suggestions, mf.currentSearchResult.b)) {
				if (tag.startsWith(tagFragment)) {
					JMenuItem tagItem = new JMenuItem(tag);
					tagItem.setForeground(TAG);
					tagItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						mf.searchF.setText(text.substring(0, spacePos) + tag +
								text.substring(caretPos));
						mf.searchF.setCaretPosition(caretPos + tag.length() - tagFragment.length());
						mf.completeMenu = null;
					}});
					add(tagItem);
				}
			}
		} else {
			// If the user just has a colon and no key, we can't make a suggestion.
			if (colonPos == 0) { return; }
			// Suggest a value
			final String key = text.substring(spacePos, colonPos - 1);
			final String valueFragment = text.substring(colonPos, caretPos);
			HashSet<String> values = new HashSet<String>(MAX_SUGGESTED_VALUES * 2);
			for (final Document d : mf.currentSearchResult.a) {
				if (d.has(key)) {
					String dValue = d.get(key);
					if (dValue.startsWith(valueFragment)) {
						values.add(
								dValue.length() > DocCellRenderer.VALUE_TRUNCATE
								? dValue.substring(0, DocCellRenderer.VALUE_TRUNCATE)
								: dValue);
					}
				}
				if (values.size() == MAX_SUGGESTED_VALUES) {
					break;
				}
			}
			ArrayList<String> valueL = new ArrayList<String>(values);
			Collections.sort(valueL);
			ArrayList<String> applicableSuggestions = new ArrayList<String>();
			for (String sugg : mf.suggestions) {
				if (sugg.startsWith(key + ":")) {
					applicableSuggestions.add(sugg.split(":", 2)[1]);
				}
			}
			valueL.addAll(0, applicableSuggestions);
			for (final String v : valueL) {
				JMenuItem valueItem = new JMenuItem(v);
				valueItem.setForeground(VALUE);
				valueItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.searchF.setText(text.substring(0, colonPos) + v + text.substring(caretPos));
					mf.searchF.setCaretPosition(caretPos + v.length() - valueFragment.length());
					mf.completeMenu = null;
				}});
				add(valueItem);
			}
		}
	}
}
