package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import static com.metalbeetle.fruitbat.util.Misc.*;

class DocumentMenuBar extends JMenuBar {
	// Manipulated by PagesViewer
	final JMenuItem openPageMI;
	final JMenuItem prevPageMI;
	final JMenuItem nextPageMI;
	final JMenuItem gotoPageMI;
	final JMenuItem assignHCNMI;
	final JMenuItem removeHCNMI;
	final JMenuItem movePageMI;

	// Manipulated by DocumentFrame
	final JMenuItem undeleteMI;
	final JMenuItem deleteMI;
	final JMenuItem addPageMI;
	final JMenuItem insertPageBeforeMI;
	final JMenuItem insertPageAfterMI;
	final JMenuItem deletePageMI;
	final JMenuItem undeletePageMI;
	final JCheckBoxMenuItem showDeletedPagesMI;

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
			undeleteMI = new JMenuItem("Undelete Document");
				fileMenu.add(undeleteMI);
				undeleteMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					try {
						df.mf.store.undelete(df.d);
						df.updateDisplay();
					} catch (Exception ex) {
						df.mf.pm.handleException(new FatalStorageException(
								"Could not undelete document.", ex), null);
					}
				}});
				undeleteMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			deleteMI = new JMenuItem("Delete Document");
				fileMenu.add(deleteMI);
				deleteMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.delete();
				}});
				deleteMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
				
		add(new EditMenu());


		JMenu pageMenu = new JMenu("Page");
			add(pageMenu);
			addPageMI = new JMenuItem("Add Page");
				pageMenu.add(addPageMI);
				addPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.numPages());
				}});
				addPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			insertPageBeforeMI = new JMenuItem("Insert Page Before This One");
				pageMenu.add(insertPageBeforeMI);
				insertPageBeforeMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.viewer.getPage());
				}});
				insertPageBeforeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			insertPageAfterMI = new JMenuItem("Insert Page After This One");
				pageMenu.add(insertPageAfterMI);
				insertPageAfterMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.viewer.getPage() + 1);
				}});
				insertPageAfterMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | Event.SHIFT_MASK));
			movePageMI = new JMenuItem("Move Page...");
				pageMenu.add(movePageMI);
				movePageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.moveCurrentPage();
				}});
				movePageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | Event.SHIFT_MASK));
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
				prevPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			nextPageMI = new JMenuItem("Next Page");
				pageMenu.add(nextPageMI);
				nextPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.nextPage();
				}});
				nextPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET,
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
			pageMenu.addSeparator();
			deletePageMI = new JMenuItem("Delete Page");
				pageMenu.add(deletePageMI);
				deletePageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.deleteCurrentPage();
				}});
				deletePageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			undeletePageMI = new JMenuItem("Undelete Page");
				pageMenu.add(undeletePageMI);
				undeletePageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.undeleteCurrentPage();
				}});
				undeletePageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			showDeletedPagesMI = new JCheckBoxMenuItem("Show Deleted Pages");
				pageMenu.add(showDeletedPagesMI);
				showDeletedPagesMI.setSelected(df.deletedPageMode);
				showDeletedPagesMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.setShowDeletedPages(showDeletedPagesMI.isSelected());
				}});
				showDeletedPagesMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));

		add(df.mf.app.wmm.getMenu(df));
	}
}
