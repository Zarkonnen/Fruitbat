package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JTextPane;
import static com.metalbeetle.fruitbat.gui.Colors.*;

/** List of suitable tags for narrowing search. */
class NarrowSearchTagsList extends TagsList {
	final MainFrame mf;
	final NarrowSearchTagsListModel m;

	NarrowSearchTagsList(MainFrame mf) {
		this(mf, new NarrowSearchTagsListModel(mf));
	}

	private NarrowSearchTagsList(final MainFrame mf, final NarrowSearchTagsListModel m) {
		super();
		this.m = m;
		this.mf = mf;
		setModel(m);
		setCellRenderer(new TagCellRenderer(mf));
	}

	@Override
	JTextPane getTagsField() {
		return mf.searchF;
	}

	@Override
	void postChange() {
		mf.search();
	}

	static class TagCellRenderer extends DefaultListCellRenderer {
		final StringBuilder sb = new StringBuilder();
		final MainFrame mf;

		TagCellRenderer(MainFrame mf) { this.mf = mf; }

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
			sb.append((value == null && mf.lastSearchKV.containsKey(key)) ? MATCHED_TAG_HTML : TAG_HTML);
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

	static class NarrowSearchTagsListModel extends AbstractListModel {
		final MainFrame mf;

		NarrowSearchTagsListModel(MainFrame mf) { this.mf = mf; }

		List<String> visibleNarrowingTags() {
			ArrayList<String> l = new ArrayList<String>();
			l.ensureCapacity(mf.currentSearchResult.narrowingTags.size());
			for (String t : mf.currentSearchResult.narrowingTags) {
				if (!t.startsWith(Fruitbat.HIDDEN_KEY_PREFIX)) {
					l.add(t);
				}
			}
			return l;
		}

		public int getSize() {
			return mf.lastSearchKeys.size() + visibleNarrowingTags().size();
		}

		public Object getElementAt(int index) {
			return
					index < mf.lastSearchKeys.size()
					? mf.lastSearchKeys.get(index)
					: visibleNarrowingTags().get(index - mf.lastSearchKeys.size());
		}

		void changed() { fireContentsChanged(this, 0, getSize()); }
	}
}
