package com.metalbeetle.fruitbat.gui;

import javax.swing.AbstractListModel;

class DocsListModel extends AbstractListModel {
	static final int MAX_DOCS_DISPLAYED = 100;

	final MainFrame mf;
	private boolean showAll = false;

	public boolean getShowAll() { return showAll; }

	public void setShowAll(boolean showAll) { this.showAll = showAll; changed(); }

	DocsListModel(MainFrame mf) { this.mf = mf; }

	public int getSize() {
		return showAll
				? mf.currentSearchResult.a.size()
				: Math.min(MAX_DOCS_DISPLAYED, mf.currentSearchResult.a.size());
	}

	public Object getElementAt(int index) {
		if (!showAll && index >= MAX_DOCS_DISPLAYED) { throw new IndexOutOfBoundsException(); }
		return mf.currentSearchResult.a.get(index);
	}

	void changed() { fireContentsChanged(this, 0, getSize()); }
}
