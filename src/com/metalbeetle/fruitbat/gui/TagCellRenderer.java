package com.metalbeetle.fruitbat.gui;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import static com.metalbeetle.fruitbat.gui.Colors.*;

class TagCellRenderer extends DefaultListCellRenderer {
	final StringBuilder sb = new StringBuilder();
	final MainFrame mf;

	public TagCellRenderer(MainFrame mf) { this.mf = mf; }

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
