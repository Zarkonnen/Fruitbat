package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.gui.setup.ConfigsListFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class WindowMenuManager implements OpenDocManager.Listener {
	private WeakHashMap<Object, WindowMenu> menus = new WeakHashMap<Object, WindowMenu>();
	private ArrayList<MainFrame> mfs = new ArrayList<MainFrame>();
	private ConfigsListFrame configsList;

	public JMenu getMenu(Object key) {
		if (!menus.containsKey(key)) {
			WindowMenu wm = new WindowMenu();
			initMenu(wm);
			menus.put(key, wm);
		}
		return menus.get(key);
	}

	private void initMenu(WindowMenu wm) {
		if (configsList != null) {
			wm.setConfigsList(configsList);
		}
		for (MainFrame mf : mfs) {
			wm.storeOpened(mf);
			for (DocumentFrame df : mf.openDocManager.openFrames.values()) {
				wm.documentOpened(mf, df);
			}
		}
	}

	public void setConfigsList(ConfigsListFrame configsList) {
		if (configsList != null) {
			this.configsList = configsList;
			for (WindowMenu wm : menus.values()) {
				wm.setConfigsList(configsList);
			}
		}
	}

	public void storeOpened(MainFrame mf) {
		mfs.add(mf);
		for (WindowMenu wm : menus.values()) {
			wm.storeOpened(mf);
		}
	}

	public void storeClosed(MainFrame mf) {
		mfs.remove(mf);
		for (WindowMenu wm : menus.values()) {
			wm.storeClosed(mf);
		}
	}

	public void documentOpened(MainFrame mf, final DocumentFrame df) {
		for (WindowMenu wm : menus.values()) {
			wm.documentOpened(mf, df);
		}
	}

	public void documentClosed(MainFrame mf, DocumentFrame df) {
		for (WindowMenu wm : menus.values()) {
			wm.documentClosed(mf, df);
		}
	}

	private class WindowMenu extends JMenu implements OpenDocManager.Listener {
		public WindowMenu() {
			super("Window");
		}

		public void setConfigsList(final ConfigsListFrame configsList) {
			configsListItem = new JMenuItem("Manage Stores");
			configsListItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					configsList.toFront();
				}
			});
			add(configsListItem);
		}

		JMenuItem configsListItem;
		HashMap<MainFrame, JMenu> storeMenus = new HashMap<MainFrame, JMenu>();
		HashMap<MainFrame, HashMap<DocumentFrame, JMenuItem>> docItems = new HashMap<MainFrame, HashMap<DocumentFrame, JMenuItem>>();

		public void storeOpened(final MainFrame mf) {
			mf.openDocManager.addListener(this);
			JMenu storeMenu = new JMenu(mf.store.toString());
			JMenuItem storeItem = new JMenuItem("Store");
			storeItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					mf.toFront();
				}
			});
			storeMenu.add(storeItem);
			storeMenus.put(mf, storeMenu);
			docItems.put(mf, new HashMap<DocumentFrame, JMenuItem>());
			add(storeMenu);
		}

		public void storeClosed(MainFrame mf) {
			remove(storeMenus.get(mf));
			storeMenus.remove(mf);
			docItems.remove(mf);
		}

		public void documentOpened(MainFrame mf, final DocumentFrame df) {
			JMenuItem docItem = new JMenuItem(df.d.toString());
			docItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					df.toFront();
				}
			});
			storeMenus.get(mf).add(docItem);
			docItems.get(mf).put(df, docItem);
		}

		public void documentClosed(MainFrame mf, DocumentFrame df) {
			storeMenus.get(mf).remove(docItems.get(mf).get(df));
			docItems.get(mf).remove(df);
		}
	}

}
