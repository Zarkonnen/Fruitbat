package com.metalbeetle.fruitbat.gui;

import javax.swing.AbstractListModel;

class DocsListModel extends AbstractListModel {
	final StoreFrame mf;

	DocsListModel(StoreFrame mf) { this.mf = mf; }

	public int getSize() {
		return mf.currentSearchResult.docs.size();
	}

	public Object getElementAt(int index) {
		return mf.currentSearchResult.docs.get(index);
	}

	void changed() { fireContentsChanged(this, 0, getSize()); }
}
