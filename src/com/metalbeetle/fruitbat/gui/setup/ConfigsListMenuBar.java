package com.metalbeetle.fruitbat.gui.setup;

import javax.swing.JMenuBar;

public class ConfigsListMenuBar extends JMenuBar {
	public ConfigsListMenuBar(ConfigsListFrame clf) {
		add(clf.app.wmm.getMenu(clf));
	}
}
