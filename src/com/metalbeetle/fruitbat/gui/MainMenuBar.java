package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.gui.blockable.BlockableCheckBoxMenuItem;
import com.metalbeetle.fruitbat.gui.blockable.BlockableMenuItem;
import com.metalbeetle.fruitbat.TestDataGenerator;
import com.metalbeetle.fruitbat.util.Misc;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import static com.metalbeetle.fruitbat.util.Misc.*;

class MainMenuBar extends JMenuBar {
	final BlockableMenuItem undeleteMI;
	final BlockableMenuItem deleteMI;
	public MainMenuBar(final MainFrame mf) {
		JMenu fileMenu = new JMenu("File");
			add(fileMenu);
			BlockableMenuItem newDocMI = new BlockableMenuItem("New Document");
				fileMenu.add(newDocMI);
				newDocMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.newDocument();
				}});
				newDocMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			BlockableMenuItem newDocAndAddPageMI = new BlockableMenuItem("New Document and Add Page");
				fileMenu.add(newDocAndAddPageMI);
				newDocAndAddPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.newDocumentAndAddPage();
				}});
				newDocAndAddPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
			deleteMI = new BlockableMenuItem("Delete Document");
				fileMenu.add(deleteMI);
				deleteMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.deleteSelectedDocument();
				}});
				deleteMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
				deleteMI.setEnabled(false);
			undeleteMI = new BlockableMenuItem("Undelete Document");
				fileMenu.add(undeleteMI);
				undeleteMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.undeleteSelectedDocument();
				}});
				undeleteMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
				undeleteMI.setEnabled(false);
				undeleteMI.setVisible(false);
			fileMenu.addSeparator();
			final BlockableCheckBoxMenuItem graveyardMI = new BlockableCheckBoxMenuItem("Show Deleted Documents");
				fileMenu.add(graveyardMI);
				graveyardMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.setShowDeletedDocs(!mf.showDeletedDocs);
					graveyardMI.setSelected(mf.showDeletedDocs);
				}});
				graveyardMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
				graveyardMI.setSelected(mf.showDeletedDocs);
			fileMenu.addSeparator();
			BlockableMenuItem closeMI = new BlockableMenuItem("Close Store");
				fileMenu.add(closeMI);
				closeMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					mf.close();
					mf.dispose();
				}});
				closeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			if (!Misc.isMac()) {
				BlockableMenuItem quitMI = new BlockableMenuItem("Quit");
					fileMenu.add(quitMI);
					quitMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						System.exit(0);
					}});
					quitMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			}

				
		// Create temp test data.
		JMenu testMenu = new JMenu("Test");
			add(testMenu);
			BlockableMenuItem createTestDataMI = new BlockableMenuItem("Create Test Data");
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
						mf.search(mf.lastSearch, MainFrame.DEFAULT_MAX_DOCS, true);
					} catch (Exception ex) { ex.printStackTrace(); }
				}});
				
	}
}
