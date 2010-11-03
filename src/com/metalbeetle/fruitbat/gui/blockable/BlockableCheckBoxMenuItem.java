package com.metalbeetle.fruitbat.gui.blockable;

import javax.swing.JCheckBoxMenuItem;

/**
 * Subclass of JCheckBoxMenuItem that allows us to easily block the UI.
 */
public class BlockableCheckBoxMenuItem extends JCheckBoxMenuItem {
	boolean blocked = false;
	boolean enabled = true;

	public BlockableCheckBoxMenuItem(String text) {
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
