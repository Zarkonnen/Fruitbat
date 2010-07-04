package com.metalbeetle.fruitbat.gui;

import javax.swing.AbstractListModel;

class TagsListModel extends AbstractListModel {
	final MainFrame mf;

	TagsListModel(MainFrame mf) { this.mf = mf; }

	public int getSize() { return mf.lastSearchKeys.size() + mf.currentSearchResult.b.size(); }

	public Object getElementAt(int index) {
		return index < mf.lastSearchKeys.size()
				? mf.lastSearchKeys.get(index)
				: mf.currentSearchResult.b.get(index - mf.lastSearchKeys.size());
	}

	void changed() { fireContentsChanged(this, 0, getSize()); }
}
