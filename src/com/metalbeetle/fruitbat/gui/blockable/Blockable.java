package com.metalbeetle.fruitbat.gui.blockable;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

public final class Blockable {
	private Blockable() {}

	public static void setBlocked(JMenuBar bar, boolean blocked) {
		for (int i = 0; i < bar.getMenuCount(); i++) {
			setBlocked(bar.getMenu(i), blocked);
		}
	}

	public static void setBlocked(JMenu m, boolean blocked) {
		for (int j = 0; j < m.getItemCount(); j++) {
			if (m.getItem(j) instanceof BlockableMenuItem) {
				((BlockableMenuItem) m.getItem(j)).setBlocked(blocked);
			}
			if (m.getItem(j) instanceof BlockableCheckBoxMenuItem) {
				((BlockableCheckBoxMenuItem) m.getItem(j)).setBlocked(blocked);
			}
			if (m.getItem(j) instanceof JMenu) {
				setBlocked((JMenu) m.getItem(j), blocked);
			}
		}
	}
}
