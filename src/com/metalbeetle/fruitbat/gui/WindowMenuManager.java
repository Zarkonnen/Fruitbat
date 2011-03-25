package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.gui.setup.ConfigsListFrame;
import com.metalbeetle.fruitbat.storage.DocumentTools;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class WindowMenuManager implements OpenDocManager.Listener {
	private WeakHashMap<Object, WindowMenu> menus = new WeakHashMap<Object, WindowMenu>();
	private ArrayList<StoreFrame> mfs = new ArrayList<StoreFrame>();
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
		for (StoreFrame mf : mfs) {
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

	public void storeOpened(StoreFrame mf) {
		mfs.add(mf);
		for (WindowMenu wm : menus.values()) {
			wm.storeOpened(mf);
		}
	}

	public void storeClosed(StoreFrame mf) {
		mfs.remove(mf);
		for (WindowMenu wm : menus.values()) {
			wm.storeClosed(mf);
		}
	}

	public void documentOpened(StoreFrame mf, final DocumentFrame df) {
		for (WindowMenu wm : menus.values()) {
			wm.documentOpened(mf, df);
		}
	}

	public void documentClosed(StoreFrame mf, DocumentFrame df) {
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
		HashMap<StoreFrame, JMenu> storeMenus = new HashMap<StoreFrame, JMenu>();
		HashMap<StoreFrame, HashMap<DocumentFrame, JMenuItem>> docItems = new HashMap<StoreFrame, HashMap<DocumentFrame, JMenuItem>>();

		public void storeOpened(final StoreFrame mf) {
			mf.openDocManager.addListener(this);
			String storeN = mf.store.toString();
			if (storeN.length() > 33) {
				storeN = storeN.substring(0, 30) + "...";
			}
			JMenu storeMenu = new JMenu(storeN);
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

		public void storeClosed(StoreFrame mf) {
			remove(storeMenus.get(mf));
			storeMenus.remove(mf);
			docItems.remove(mf);
		}

		public void documentOpened(StoreFrame mf, final DocumentFrame df) {
			JMenuItem docItem = new JMenuItem(DocumentTools.getDocTitle(df.d));
			docItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					df.toFront();
				}
			});
			storeMenus.get(mf).add(docItem);
			docItems.get(mf).put(df, docItem);
		}

		public void documentClosed(StoreFrame mf, DocumentFrame df) {
			storeMenus.get(mf).remove(docItems.get(mf).get(df));
			docItems.get(mf).remove(df);
		}
	}

}
