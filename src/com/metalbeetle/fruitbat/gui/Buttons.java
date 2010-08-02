package com.metalbeetle.fruitbat.gui;

import java.awt.Image;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;

class Buttons {
	static JButton getIconButton(String iconName) {
		Image img = null;
		try {
			img = ImageIO.read(Buttons.class.getResourceAsStream(iconName));
		} catch (Exception e) {}
		if (img != null) {
			return new JButton(new ImageIcon(img));
		} else {
			return new JButton("[" + iconName + "]");
		}
	}
}
