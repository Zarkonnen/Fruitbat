package com.metalbeetle.fruitbat;

import apple.dts.samplecode.osxadapter.OSXAdapter;
import com.metalbeetle.fruitbat.gui.AboutWindow;
import com.metalbeetle.fruitbat.gui.Dialogs;
import com.metalbeetle.fruitbat.gui.MainFrame;
import com.metalbeetle.fruitbat.gui.ShortcutOverlay;
import com.metalbeetle.fruitbat.gui.SplashWindow;
import com.metalbeetle.fruitbat.gui.setup.ConfigsListFrame;
import com.metalbeetle.fruitbat.prefs.SavedStoreConfigs;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.util.Misc;
import com.metalbeetle.fruitbat.util.Pair;
import com.metalbeetle.fruitbat.util.StringPool;
import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

/** Application instance. */
public class Fruitbat {
	public static final String VERSION = "1.0b";
	public static final String ABOUT = "by David Stark, 2010";
	public static final String HIDDEN_KEY_PREFIX = ":";
	public static final String DATE_KEY = "d";
	public static final String CREATION_DATE_KEY = HIDDEN_KEY_PREFIX + "cd";
	public static final String ALIVE_KEY = HIDDEN_KEY_PREFIX + "a";
	public static final String DEAD_KEY = HIDDEN_KEY_PREFIX + "d";
	static final int POOL_CUTOFF = 128;

	ProgressMonitor pm;
	final StringPool stringPool = new StringPool(POOL_CUTOFF);
	final HashMap<StoreConfig, MainFrame> configToMainframe = new HashMap<StoreConfig, MainFrame>();
	final ConfigsListFrame configsList;
	public final ShortcutOverlay shortcutOverlay = new ShortcutOverlay();
	volatile boolean shuttingDown = false;

	public MainFrame openStore(StoreConfig sc) {
		if (!configToMainframe.containsKey(sc)) {
			pm.showProgressBar("Loading store", "", -1);
			try {
				MainFrame mf = new MainFrame(this, sc.init(pm), sc);
				configToMainframe.put(sc, mf);
			} catch (Exception e) {
				pm.handleException(e, null);
				return null;
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
		if (configToMainframe.isEmpty() && !shuttingDown) {
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
		if (Misc.isMac()) {
			try {
				OSXAdapter.setQuitHandler(this, Fruitbat.class.getMethod("closeIfAble"));
				OSXAdapter.setAboutHandler(this, Fruitbat.class.getMethod("showAbout"));
			} catch (Exception e) {
				pm.handleException(e, null);
				System.exit(1);
			}
		}
		pm.showProgressBar("Welcome to Fruitbat", "", -1);
		configsList = new ConfigsListFrame(this, pm);
		configsList.setLocationRelativeTo(null);
		configsList.setVisible(true);
		try {
			for (Pair<StoreConfig, Preferences> openStores : SavedStoreConfigs.getOpenStores()) {
				MainFrame mf = openStore(openStores.a);
				if (mf != null) { mf.readPrefs(openStores.b); }
			}
		} catch (Exception e) {
			pm.handleException(new Exception("Couldn't load open stores.", e), null);
		}
		pm.hideProgressBar();
		pm = new Dialogs();
		for (MainFrame mf : configToMainframe.values()) {
			mf.setProgressMonitor(pm);
		}
	}

	public boolean closeIfAble() {
		close();
		return true;
	}

	public void showAbout() {
		AboutWindow aw = new AboutWindow();
		aw.setLocationRelativeTo(null);
		aw.setVisible(true);
	}

	public void close() {
		new Thread("Closing Fruitbat") { @Override public void run() {
			if (shuttingDown) { return; }
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
				for (Iterator<MainFrame> it = configToMainframe.values().iterator(); it.hasNext();) {
					MainFrame mf = it.next();
					if (!mf.getUIBusy()) {
						it.remove();
						try {
							mf.close();
						} catch (Exception e) {
							pm.handleException(e, null);
						}
					}
				}
				for (MainFrame mf : configToMainframe.values()) {
					while (mf.getUIBusy()) {
						pm.progress("Waiting for " + mf.getStore(), -1);
						try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
					}
					try {
						mf.close();
					} catch (Exception e) {
						pm.handleException(e, null);
					}
				}
			} finally {
				pm.hideProgressBar();
				System.exit(0);
			}
		}}.start();
	}
}
