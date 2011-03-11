package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.DocumentTools;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JTextPane;
import static com.metalbeetle.fruitbat.gui.Colors.*;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;


class SuggestedTagsList extends TagsList {
	final DocumentFrame df;
	final SuggestedTagsListModel lm;

	SuggestedTagsList(DocumentFrame df) {
		this.df = df;
		setModel(lm = new SuggestedTagsListModel(this));
		setCellRenderer(new TagCellRenderer());
		update();
	}

	@Override
	JTextPane getTagsField() {
		return df.tagsF;
	}

	@Override
	void postChange() {
		df.saveTags();
		lm.update();
	}

	void update() {
		lm.update();
	}

	static class TagCellRenderer extends DefaultListCellRenderer {
		final StringBuilder sb = new StringBuilder();
		@Override
		public Component getListCellRendererComponent(JList list, Object o, int index,
				boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, o, index, false, false);
			String key = (String) o;
			String value = null;
			if (key.contains(":")) {
				String[] kv = key.split(":", 2);
				key = kv[0];
				value = ":" + kv[1];
			}
			sb.setLength(0);
			sb.append("<html><font color=\"");
			sb.append(TAG_HTML);
			sb.append("\">");
			sb.append(key);
			sb.append("</font>");
			if (value != null) {
				sb.append("<font color=\"");
				sb.append(VALUE_HTML);
				sb.append("\">");
				sb.append(value);
				sb.append("</font>");
			}
			sb.append("</html>");
			setText(sb.toString());
			return this;
		}
	}

	static class SuggestedTagsListModel extends AbstractListModel {
		final SuggestedTagsList l;

		SuggestedTagsListModel(SuggestedTagsList l) {
			this.l = l;
		}

		String cProf1 = "";
		String cProf2 = "";

		ArrayList<String> suggestedTags = new ArrayList<String>();
		HashSet<String> colorProfileSuggestedTags = new HashSet<String>();

		public int getSize() { return suggestedTags.size(); }

		public Object getElementAt(int i) { return suggestedTags.get(i); }

		void update() {
			try {
				suggestedTags.clear();
				HashSet<String> usedKeys = l.getUsedKeys();
				DocIndex di = l.df.mf.store.getIndex();

				// By profile
				String newCProf1 = l.df.d.has(DocumentTools.COLOR_PROFILE_1)
						? l.df.d.get(DocumentTools.COLOR_PROFILE_1)
						: "";

				String newCProf2 = l.df.d.has(DocumentTools.COLOR_PROFILE_2)
						? l.df.d.get(DocumentTools.COLOR_PROFILE_2)
						: "";

				if (!cProf1.equals(newCProf1) || !cProf2.equals(newCProf2)) {
					cProf1 = newCProf1;
					cProf2 = newCProf2;
					colorProfileSuggestedTags.clear();
					if (cProf1.length() > 0) {
						colorProfileSuggestedTags.addAll(
								di.search(m(p(DocumentTools.COLOR_PROFILE_1, cProf1)),
								DocIndex.ALL_DOCS).narrowingTags);
					}
					if (cProf2.length() > 0) {
						colorProfileSuggestedTags.addAll(
								di.search(m(p(DocumentTools.COLOR_PROFILE_2, cProf2)),
								DocIndex.ALL_DOCS).narrowingTags);
					}
				}

				suggestedTags.addAll(colorProfileSuggestedTags);

				// Tag coocurrence

				HashMap<String, String> terms = new HashMap<String, String>();
				for (String s : usedKeys) {
					if (!s.equals(Fruitbat.DATE_KEY) && di.isKey(s)) {
						terms.put(s, null);
					}
				}

				if (terms.size() > 0) {
					List<String> coocs = di.search(terms, DocIndex.ALL_DOCS).narrowingTags;
					coocs.removeAll(suggestedTags);
					suggestedTags.addAll(coocs);
				}

				suggestedTags.removeAll(usedKeys);
				suggestedTags.remove(Fruitbat.DATE_KEY);

				for (Iterator<String> it = suggestedTags.iterator(); it.hasNext();) {
					if (it.next().startsWith(Fruitbat.HIDDEN_KEY_PREFIX)) { it.remove(); }
				}
				Collections.sort(suggestedTags);

				// Datestamp
				if (!usedKeys.contains(Fruitbat.DATE_KEY)) {
					if (l.df.d.has(Fruitbat.CREATION_DATE_KEY)) {
						suggestedTags.add(0, Fruitbat.DATE_KEY + ":" +
								l.df.d.get(Fruitbat.CREATION_DATE_KEY));
					} else {
						suggestedTags.add(0, Fruitbat.DATE_KEY + ":" + currentDateString());
					}
				}

				fireContentsChanged(this, 0, getSize());
			} catch (FatalStorageException e) {
				l.df.mf.handleException(e);
			}
		}
	}
}
