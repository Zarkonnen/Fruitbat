package com.metalbeetle.fruitbat.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import javax.swing.JPanel;

public class AllInterceptingPane extends JPanel {
	static final Color HAZE = new Color(127, 127, 127, 63);

	public AllInterceptingPane() {
		addKeyListener(new KeyAdapter() {});
		addMouseListener(new MouseAdapter() {});
		setOpaque(false);
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(HAZE);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
