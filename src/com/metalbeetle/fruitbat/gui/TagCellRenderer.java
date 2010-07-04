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
	public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, false, false);
		sb.setLength(0);
		sb.append("<html><font color=\"");
		sb.append(mf.lastSearchKV.containsKey((String) value) ? MATCHED_TAG_HTML : TAG_HTML);
		sb.append("\">");
		sb.append(value);
		sb.append("</font></html>");
		setText(sb.toString());
		return this;
	}
}
