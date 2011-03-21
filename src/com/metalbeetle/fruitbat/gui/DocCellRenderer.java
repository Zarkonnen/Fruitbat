package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.DocumentTools;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import static com.metalbeetle.fruitbat.gui.Colors.*;

class DocCellRenderer extends DefaultListCellRenderer {
	static final int VALUE_TRUNCATE = 20;
	final StringBuilder sb = new StringBuilder();
	final MainFrame mf;

	DocCellRenderer(MainFrame mf) {
		this.mf = mf;
	}

	class KeyComparator implements Comparator<String> {
		public int compare(String k1, String k2) {
			if (k1.equals(Fruitbat.DATE_KEY)) { return -1; }
			if (k2.equals(Fruitbat.DATE_KEY)) { return 1; }
			boolean c1 = mf.lastSearchKV.get(k1) != null;
			boolean c2 = mf.lastSearchKV.get(k2) != null;
			if (c1 == c2) {
				return k1.compareToIgnoreCase(k2);
			}
			return c1 ? -1 : 1;
		}
	}

	final KeyComparator kc = new KeyComparator();

	@Override
	public Component getListCellRendererComponent(JList list, Object o, int index,
			boolean isSelected, boolean cellHasFocus)
	{
		try {
			super.getListCellRendererComponent(list, o, index, cellHasFocus && isSelected,
					cellHasFocus);
			Document d = (Document) o;
			sb.setLength(0);
			sb.append("<html>");
			String profile = "ffffff";
			if (d.has(DocumentTools.COLOR_PROFILE_1)) {
				profile = d.get(DocumentTools.COLOR_PROFILE_1);
				if (profile.equals("")) {
					profile = "ffffff";
				}
			}
			String p1 = profile.substring(0, 6);
			for (int i = 0; i < 2; i++) {
				sb.append("<font bgcolor=\"");
				sb.append(p1);
				sb.append("\" color=\"");
				sb.append(p1);
				sb.append("\">i</font>");
				if (profile.length() > 6) {
					p1 = profile.substring(6, 12);
				}
			}
			if (d.has(Fruitbat.DEAD_KEY)) {
				sb.append("<s>");
			}
			boolean hasKeys = false;
			ArrayList<String> keys = new ArrayList<String>(d.keys());
			Collections.sort(keys, kc);
			for (String key : keys) {
				if (key.startsWith(Fruitbat.HIDDEN_KEY_PREFIX)) { continue; }
				hasKeys = true;
				sb.append(" <font color=\"");
				sb.append(mf.lastSearchKV.containsKey(key) ? MATCHED_TAG_HTML : TAG_HTML);
				sb.append("\">");
				sb.append(key);
				sb.append("</font>");
				if (mf.lastSearchKV.get(key) != null) {
					String value = d.get(key);
					if (value.length() > VALUE_TRUNCATE) {
						value = value.substring(0, VALUE_TRUNCATE - 3) + "...";
					}
					sb.append("<font color=\"");
					sb.append(VALUE_HTML);
					sb.append("\">:");
					sb.append(value);
					sb.append("</font>");
				}
			}
			if (!hasKeys) {
				sb.append(" <font color=\"");
				sb.append(IGNORED_TAG_HTML);
				sb.append("\">[ no tags ]</font>");
			}
			if (mf.showDeletedDocs) {
				sb.append("</s>");
			}
			sb.append("</html>");
			setText(sb.toString());
			return this;
		} catch (FatalStorageException e) {
			mf.handleException(e); return this;
		}
	}

}
