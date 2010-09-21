package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.TestDataGenerator;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import static com.metalbeetle.fruitbat.util.Misc.*;

class MainMenuBar extends JMenuBar {
	public MainMenuBar(final MainFrame mf) {
		JMenu fileMenu = new JMenu("File");
			add(fileMenu);
			JMenuItem newDocMI = new JMenuItem("New Document");
				fileMenu.add(newDocMI);
				newDocMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.newDocument();
				}});
				newDocMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.addSeparator();
			JMenuItem graveyardMI = new JMenuItem("View Deleted Documents");
				fileMenu.add(graveyardMI);
				graveyardMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					if (!mf.graveyard.isVisible()) {
						mf.graveyard.setLocationRelativeTo(null);
					}
					mf.graveyard.setVisible(true);
					mf.graveyard.toFront();
				}});
				graveyardMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.addSeparator();
			JMenuItem closeMI = new JMenuItem("Close Store");
				fileMenu.add(closeMI);
				closeMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.close();
					mf.dispose();
				}});
				closeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		JMenu testMenu = new JMenu("Test");
			add(testMenu);
			JMenuItem createTestDataMI = new JMenuItem("Create Test Data");
				testMenu.add(createTestDataMI);
				createTestDataMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					try {
						int quantity = integer(mf.pm.askQuestion(
							"How many test documents would you like?",
							"How many test documents would you like?",
							"100"));
						int year = integer(mf.pm.askQuestion(
							"Which year?",
							"Which year should these docs be from?",
							"2009"));
						TestDataGenerator.generate(mf.store, quantity, year);
						mf.search(mf.lastSearch, MainFrame.DEFAULT_MAX_DOCS, /*force*/true);
					} catch (Exception ex) { ex.printStackTrace(); }
				}});
	}
}
