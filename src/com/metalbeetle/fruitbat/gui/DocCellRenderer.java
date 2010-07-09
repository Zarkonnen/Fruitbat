package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
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
		super.getListCellRendererComponent(list, o, index, cellHasFocus && isSelected, cellHasFocus);
		Document d = (Document) o;
		sb.setLength(0);
		sb.append("<html>");
		for (String key : d.keys()) {
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
		sb.append("</html>");
		setText(sb.toString());
		return this;
	}

}
