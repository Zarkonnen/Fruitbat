package com.metalbeetle.fruitbat.gui.blockable;

import javax.swing.JMenuItem;

/**
 * Subclass of JMenuItem that allows us to easily block the UI.
 */
public class BlockableMenuItem extends JMenuItem {
	boolean blocked = false;
	boolean enabled = true;

	public BlockableMenuItem(String text) {
		super(text);
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		super.setEnabled(enabled && !blocked);
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
		super.setEnabled(enabled && !blocked);
	}
}
