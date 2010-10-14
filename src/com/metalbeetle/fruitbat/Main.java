package com.metalbeetle.fruitbat;

import javax.swing.UIManager;

public class Main {
	public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // We don't care.
        }
		new Fruitbat();
	}
}
