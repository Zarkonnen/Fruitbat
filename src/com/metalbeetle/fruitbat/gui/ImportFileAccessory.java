package com.metalbeetle.fruitbat.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;

class ImportFileAccessory extends Box {

	final JCheckBox deleteAfterAdding;
	final JCheckBox retainedOriginal;

	public ImportFileAccessory() {
		super(BoxLayout.Y_AXIS);
		add(deleteAfterAdding = new JCheckBox("Delete original"));
		deleteAfterAdding.setSelected(true);
		deleteAfterAdding.setToolTipText("Deletes the original file after adding it to " + "the document as a page.");
		add(retainedOriginal = new JCheckBox("Assign hardcopy number"));
		retainedOriginal.setToolTipText("If you are keeping the original paper copy of " + "this page, this gives it an unique number you can file it under.");
	}
}
