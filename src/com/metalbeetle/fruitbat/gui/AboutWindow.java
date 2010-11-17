package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;

public class AboutWindow extends JFrame {
	public AboutWindow() {
		setSize(400, 320);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

	@Override
	public void paint(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 400, 320);
		if (SplashWindow.SPLASH_IMG != null) {
			g.drawImage(SplashWindow.SPLASH_IMG, 0, 0, this);
		}
		g.setColor(Color.GRAY);
		int w = g.getFontMetrics().stringWidth(Fruitbat.VERSION);
		g.drawString(Fruitbat.VERSION, 400 - w - 10, 300);
		g.drawString(Fruitbat.ABOUT, 10, 300);
	}
}
