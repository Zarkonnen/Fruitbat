package com.metalbeetle.fruitbat.gui;

import javax.swing.AbstractListModel;

class TagsListModel extends AbstractListModel {
	final MainFrame mf;

	TagsListModel(MainFrame mf) { this.mf = mf; }

	public int getSize() {
		return mf.lastSearchKeys.size() + mf.suggestions.size() +
				mf.currentSearchResult.narrowingTags.size();
	}

	public Object getElementAt(int index) {
		return
				index < mf.lastSearchKeys.size()
				? mf.lastSearchKeys.get(index)
				: (index - mf.lastSearchKeys.size()) < mf.suggestions.size()
				? mf.suggestions.get(index - mf.lastSearchKeys.size())
				: mf.currentSearchResult.narrowingTags.get(index - mf.lastSearchKeys.size() - mf.suggestions.size());
	}

	void changed() { fireContentsChanged(this, 0, getSize()); }
}
