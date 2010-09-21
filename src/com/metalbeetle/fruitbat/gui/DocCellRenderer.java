package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.Component;
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
			if (d.has(DocumentFrame.COLOR_PROFILE_1)) {
				profile = d.get(DocumentFrame.COLOR_PROFILE_1);
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
			boolean hasKeys = false;
			for (String key : d.keys()) {
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
			sb.append("</html>");
			setText(sb.toString());
			return this;
		} catch (FatalStorageException e) {
			mf.handleException(e); return this;
		}
	}

}
