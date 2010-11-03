package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.gui.blockable.BlockableCheckBoxMenuItem;
import com.metalbeetle.fruitbat.gui.blockable.BlockableMenuItem;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import static com.metalbeetle.fruitbat.util.Misc.*;

class DocumentMenuBar extends JMenuBar {
	// Manipulated by PagesViewer
	final BlockableMenuItem openPageMI;
	final BlockableMenuItem prevPageMI;
	final BlockableMenuItem nextPageMI;
	final BlockableMenuItem gotoPageMI;
	final BlockableMenuItem assignHCNMI;
	final BlockableMenuItem removeHCNMI;
	final BlockableMenuItem movePageMI;

	// Manipulated by DocumentFrame
	final BlockableMenuItem undeleteMI;
	final BlockableMenuItem deleteMI;
	final BlockableMenuItem addPageMI;
	final BlockableMenuItem insertPageBeforeMI;
	final BlockableMenuItem insertPageAfterMI;
	final BlockableMenuItem deletePageMI;
	final BlockableMenuItem undeletePageMI;
	final BlockableCheckBoxMenuItem showDeletedPagesMI;

	public DocumentMenuBar(final DocumentFrame df) {
		JMenu fileMenu = new JMenu("File");
			add(fileMenu);
			BlockableMenuItem newDocMI = new BlockableMenuItem("New Document");
				fileMenu.add(newDocMI);
				newDocMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.mf.newDocument();
				}});
				newDocMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			BlockableMenuItem closeMI = new BlockableMenuItem("Close");
				fileMenu.add(closeMI);
				closeMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.close();
				}});
				closeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			fileMenu.addSeparator();
			undeleteMI = new BlockableMenuItem("Undelete Document");
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
			deleteMI = new BlockableMenuItem("Delete Document");
				fileMenu.add(deleteMI);
				deleteMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.delete();
				}});
				deleteMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		JMenu pageMenu = new JMenu("Page");
			add(pageMenu);
			addPageMI = new BlockableMenuItem("Add Page");
				pageMenu.add(addPageMI);
				addPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.numPages());
				}});
				addPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			insertPageBeforeMI = new BlockableMenuItem("Insert Page Before This One");
				pageMenu.add(insertPageBeforeMI);
				insertPageBeforeMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.viewer.getPage());
				}});
				insertPageBeforeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			insertPageAfterMI = new BlockableMenuItem("Insert Page After This One");
				pageMenu.add(insertPageAfterMI);
				insertPageAfterMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.insertPagesAt(df.viewer.getPage() + 1);
				}});
				insertPageAfterMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | Event.SHIFT_MASK));
			movePageMI = new BlockableMenuItem("Move Page...");
				pageMenu.add(movePageMI);
				movePageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.moveCurrentPage();
				}});
				movePageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | Event.SHIFT_MASK));
			assignHCNMI = new BlockableMenuItem("Assign Hardcopy Number");
				pageMenu.add(assignHCNMI);
				assignHCNMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.assignHardcopyNumber();
				}});
			removeHCNMI = new BlockableMenuItem("Remove Hardcopy Number");
				pageMenu.add(removeHCNMI);
				removeHCNMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.removeHardcopyNumber();
				}});
			pageMenu.addSeparator();
			openPageMI = new BlockableMenuItem("Open Page in Editor");
				pageMenu.add(openPageMI);
				openPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.openPage();
				}});
				openPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() |
						KeyEvent.SHIFT_MASK));
			prevPageMI = new BlockableMenuItem("Previous Page");
				pageMenu.add(prevPageMI);
				prevPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.prevPage();
				}});
				prevPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			nextPageMI = new BlockableMenuItem("Next Page");
				pageMenu.add(nextPageMI);
				nextPageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.viewer.nextPage();
				}});
				nextPageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			gotoPageMI = new BlockableMenuItem("Go to Page...");
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
			deletePageMI = new BlockableMenuItem("Delete Page");
				pageMenu.add(deletePageMI);
				deletePageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.deleteCurrentPage();
				}});
				deletePageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			undeletePageMI = new BlockableMenuItem("Undelete Page");
				pageMenu.add(undeletePageMI);
				undeletePageMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.undeleteCurrentPage();
				}});
				undeletePageMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			showDeletedPagesMI = new BlockableCheckBoxMenuItem("Show Deleted Pages");
				pageMenu.add(showDeletedPagesMI);
				showDeletedPagesMI.setSelected(df.deletedPageMode);
				showDeletedPagesMI.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					df.setShowDeletedPages(showDeletedPagesMI.isSelected());
				}});
				showDeletedPagesMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
	}
}
