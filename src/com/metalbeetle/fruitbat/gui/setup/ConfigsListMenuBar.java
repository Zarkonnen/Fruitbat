package com.metalbeetle.fruitbat.gui.setup;

import com.metalbeetle.fruitbat.gui.EditMenu;
import javax.swing.JMenuBar;

public class ConfigsListMenuBar extends JMenuBar {
	public ConfigsListMenuBar(ConfigsListFrame clf) {
		add(new EditMenu(clf.app));
		add(clf.app.wmm.getMenu(clf));
	}
}
