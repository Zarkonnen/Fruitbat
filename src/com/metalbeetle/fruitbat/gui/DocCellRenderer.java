package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import static com.metalbeetle.fruitbat.gui.Colors.*;

class DocCellRenderer extends DefaultListCellRenderer {
	final StringBuilder sb = new StringBuilder();
	final MainFrame mf;

	DocCellRenderer(MainFrame mf) {
		this.mf = mf;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, false, false);
		Document d = (Document) value;
		sb.setLength(0);
		sb.append("<html>");
		for (String k : d.keys()) {
			sb.append(" <font color=\"");
			sb.append(mf.lastSearchKV.containsKey(k) ? MATCHED_TAG_HTML : TAG_HTML);
			sb.append("\">");
			sb.append(k);
			sb.append("</font>");
		}
		sb.append("</html>");
		setText(sb.toString());
		return this;
	}

}
