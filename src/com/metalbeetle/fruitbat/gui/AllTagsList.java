package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JTextPane;
import static com.metalbeetle.fruitbat.gui.Colors.*;

class AllTagsList extends TagsList {
	final DocumentFrame df;
	final AllTagsListModel lm;

	AllTagsList(DocumentFrame df) {
		this.df = df;
		setModel(lm = new AllTagsListModel(this));
		setCellRenderer(new TagCellRenderer(this));
		update();
	}

	@Override
	JTextPane getTagsField() {
		return df.tagsF;
	}

	@Override
	void postChange() {
		df.saveTagsAndNotes();
		lm.update();
	}

	void update() {
		lm.update();
	}

	List<String> getUnusedTags() { return lm.unusedTags; }

	static class TagCellRenderer extends DefaultListCellRenderer {
		final StringBuilder sb = new StringBuilder();
		final AllTagsList l;

		TagCellRenderer(AllTagsList l) { this.l = l; }

		@Override
		public Component getListCellRendererComponent(JList list, Object o, int index,
				boolean isSelected, boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, o, index, false, false);
			String tag = (String) o;
			sb.setLength(0);
			sb.append("<html><font color=\"");
			sb.append(index < l.lm.usedTags.size() ? TAG_HTML : UNUSED_TAG_HTML);
			sb.append("\">");
			sb.append(tag);
			sb.append("</font>");
			sb.append("</html>");
			setText(sb.toString());
			return this;
		}
	}

	static class AllTagsListModel extends AbstractListModel {
		final AllTagsList l;

		AllTagsListModel(AllTagsList l) {
			this.l = l;
		}

		ArrayList<String> usedTags = new ArrayList<String>();
		List<String> unusedTags = new ArrayList<String>();

		public int getSize() {
			return usedTags.size() + unusedTags.size();
		}

		public Object getElementAt(int i) {
			return i < usedTags.size() ? usedTags.get(i) : unusedTags.get(i - usedTags.size());
		}

		void update() {
			try {
				usedTags.clear();
				usedTags.addAll(l.getUsedKeys());
				Collections.sort(usedTags);
				unusedTags = l.df.sf.store.getIndex().allKeys();
				unusedTags.removeAll(usedTags);
				for (Iterator<String> it = unusedTags.iterator(); it.hasNext();) {
					if (it.next().startsWith(Fruitbat.HIDDEN_KEY_PREFIX)) { it.remove(); }
				}
				fireContentsChanged(this, 0, getSize());
			} catch (FatalStorageException e) {
				l.df.sf.handleException(e);
			}
		}
	}
}
