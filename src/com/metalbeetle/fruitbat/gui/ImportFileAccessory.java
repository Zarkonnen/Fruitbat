package com.metalbeetle.fruitbat.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;

class ImportFileAccessory extends Box {

	final JCheckBox storeCopy;
	final JCheckBox retainedOriginal;

	public ImportFileAccessory() {
		super(BoxLayout.Y_AXIS);
		add(storeCopy = new JCheckBox("Store a copy"));
		storeCopy.setSelected(false);
		storeCopy.setToolTipText("Stores a copy of the selected document(s) instead of " +
				"moving the originals.");
		add(retainedOriginal = new JCheckBox("Assign hardcopy number"));
		retainedOriginal.setToolTipText("If you are keeping the original paper copy of this " +
				"page, this gives the page a unique number you can file it under.");
	}
}
