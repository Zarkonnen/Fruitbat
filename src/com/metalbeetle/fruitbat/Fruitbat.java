package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.gui.Dialogs;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.gui.MainFrame;
import com.metalbeetle.fruitbat.gui.ShortcutOverlay;
import com.metalbeetle.fruitbat.gui.SplashWindow;
import com.metalbeetle.fruitbat.gui.setup.ConfigsListFrame;
import com.metalbeetle.fruitbat.prefs.SavedStoreConfigs;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.util.Pair;
import com.metalbeetle.fruitbat.util.StringPool;
import java.util.HashMap;
import java.util.prefs.Preferences;

/** Application instance. */
public class Fruitbat {
	public static final String VERSION = "0.2.0";
	public static final String HIDDEN_KEY_PREFIX = ":";
	public static final String DATE_KEY = "d";
	public static final String CREATION_DATE_KEY = HIDDEN_KEY_PREFIX + "cd";
	static final int POOL_CUTOFF = 128;

	ProgressMonitor pm;
	final StringPool stringPool = new StringPool(POOL_CUTOFF);
	final HashMap<StoreConfig, MainFrame> configToMainframe = new HashMap<StoreConfig, MainFrame>();
	final ConfigsListFrame configsList;
	public final ShortcutOverlay shortcutOverlay = new ShortcutOverlay();
	boolean shuttingDown = false;

	public MainFrame openStore(StoreConfig sc) {
		if (!configToMainframe.containsKey(sc)) {
			pm.showProgressBar("Loading store", "", -1);
			try {
				MainFrame mf = new MainFrame(this, sc.init(pm), pm, sc);
				configToMainframe.put(sc, mf);
			} catch (Exception e) {
				pm.handleException(e, null);
			} finally {
				pm.hideProgressBar();
			}
		}
		if (!configToMainframe.get(sc).isVisible()) {
			configToMainframe.get(sc).setLocationRelativeTo(null);
			configToMainframe.get(sc).setVisible(true);
		}
		configToMainframe.get(sc).toFront();
		return configToMainframe.get(sc);
	}

	public void storeClosed(MainFrame mf) {
		configToMainframe.remove(mf.getConfig());
		if (configToMainframe.size() == 0 && !shuttingDown) {
			configsList.setVisible(true);
		}
	}

	void setProgressMonitor(ProgressMonitor pm) {
		for (MainFrame mf : configToMainframe.values()) {
			mf.setProgressMonitor(pm);
		}
	}

	public Fruitbat() {
		pm = new SplashWindow();
		pm.showProgressBar("Welcome to Fruitbat", "", -1);
		configsList = new ConfigsListFrame(this, pm);
		configsList.setLocationRelativeTo(null);
		configsList.setVisible(true);
		try {
			for (Pair<StoreConfig, Preferences> openStores : SavedStoreConfigs.getOpenStores()) {
				openStore(openStores.a).readPrefs(openStores.b);
			}
		} catch (Exception e) {
			pm.handleException(new Exception("Couldn't load open stores.", e), null);
		}
		pm.hideProgressBar();
		pm = new Dialogs();
		for (MainFrame mf : configToMainframe.values()) {
			mf.setProgressMonitor(pm);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shuttingDown = true;
				try {
					shortcutOverlay.shutdown();
				} catch (Exception e) {
					// Meh.
				}
				try {
					SavedStoreConfigs.setOpenStores(configToMainframe);
				} catch (Exception e) {
					// Meh.
				}
				pm.showProgressBar("Closing stores", "", -1);
				try {
					for (MainFrame mf : configToMainframe.values()) {
						try {
							mf.close();
						} catch (Exception e) {
							pm.handleException(e, null);
						}
					}
				} finally {
					pm.hideProgressBar();
				}
			}
		});
	}
}
