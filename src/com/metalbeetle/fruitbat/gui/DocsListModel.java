package com.metalbeetle.fruitbat.gui;

import javax.swing.AbstractListModel;

class DocsListModel extends AbstractListModel {
	final MainFrame mf;

	DocsListModel(MainFrame mf) { this.mf = mf; }

	public int getSize() { return mf.currentSearchResult.a.size(); }

	public Object getElementAt(int index) {
		return mf.currentSearchResult.a.get(index);
	}

	void changed() { fireContentsChanged(this, 0, getSize()); }
}
