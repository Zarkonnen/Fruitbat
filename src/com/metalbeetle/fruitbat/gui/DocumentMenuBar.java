package com.metalbeetle.fruitbat.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import static com.metalbeetle.fruitbat.util.Misc.*;

class DocumentMenuBar extends JMenuBar {
	final JMenuItem openPageMI;
	final JMenuItem prevPageMI;
	final JMenuItem nextPageMI;
	final JMenuItem gotoPageMI;
	final JMenuItem assignHCNMI;
	final JMenuItem removeHCNMI;

	public DocumentMenuBar(final DocumentFrame df) {
		JMenu fileMenu = new JMenu("File");
			add(fileMenu);
			JMenuItem newDocMI = new JMenuItem("New Document");
				fileMenu.add(newDocMI);
				newDocMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.mf.newDocument();
				}});
				newDocMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			JMenuItem closeMI = new JMenuItem("Close");
				fileMenu.add(closeMI);
				closeMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.close();
				}});
				closeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.addSeparator();
			JMenuItem deleteMI = new JMenuItem("Delete document");
				fileMenu.add(deleteMI);
				deleteMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.delete();
				}});
		JMenu pageMenu = new JMenu("Page");
			add(pageMenu);
			JMenuItem addPageMI = new JMenuItem("Add Page");
				pageMenu.add(addPageMI);
				addPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.numPages());
				}});
				addPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			JMenuItem insertPageBeforeMI = new JMenuItem("Insert Page Before This One");
				pageMenu.add(insertPageBeforeMI);
				insertPageBeforeMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.viewer.getPage());
				}});
			JMenuItem insertPageAfterMI = new JMenuItem("Insert Page After This One");
				pageMenu.add(insertPageAfterMI);
				insertPageAfterMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.viewer.getPage() + 1);
				}});
			assignHCNMI = new JMenuItem("Assign Hardcopy Number");
				pageMenu.add(assignHCNMI);
				assignHCNMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.assignHardcopyNumber();
				}});
			removeHCNMI = new JMenuItem("Remove Hardcopy Number");
				pageMenu.add(removeHCNMI);
				removeHCNMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.removeHardcopyNumber();
				}});
			pageMenu.addSeparator();
			openPageMI = new JMenuItem("Open Page in Editor");
				pageMenu.add(openPageMI);
				openPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.openPage();
				}});
				openPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() |
						KeyEvent.SHIFT_MASK));
			prevPageMI = new JMenuItem("Previous Page");
				pageMenu.add(prevPageMI);
				prevPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.prevPage();
				}});
				prevPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			nextPageMI = new JMenuItem("Next Page");
				pageMenu.add(nextPageMI);
				nextPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.nextPage();
				}});
				nextPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			gotoPageMI = new JMenuItem("Go to Page...");
				pageMenu.add(gotoPageMI);
				gotoPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					String pageS = df.mf.pm.askQuestion("Go to Page",
							"Which page do you want to go to?", "");
					int page = -1;
					try { page = integer(pageS); } catch (Exception ex) {}
					if (page != -1) {
						df.viewer.setPage(page - 1);
					}
				}});
				gotoPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}
}
