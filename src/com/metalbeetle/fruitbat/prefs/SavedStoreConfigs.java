package com.metalbeetle.fruitbat.prefs;

import com.metalbeetle.fruitbat.gui.MainFrame;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import com.metalbeetle.fruitbat.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;


public final class SavedStoreConfigs {
	private SavedStoreConfigs() {}

	public static List<StoreConfig> getSavedStoreConfigs(ProgressMonitor pm) throws BackingStoreException, StoreConfigInvalidException {
		Preferences p = Preferences.userNodeForPackage(SavedStoreConfigs.class).
				node("savedStoreConfigs");
		ArrayList<StoreConfig> l = new ArrayList<StoreConfig>();
		for (String name : p.keys()) {
			try {
				l.add(new StoreConfig(p.get(name, null)));
			} catch (Exception e) {
				pm.handleException(new Exception("Cannot load saved store \"" + name + "\". It " +
						"may have moved or otherwise become inaccessible.", e), null);
			}
		}
		return l;
	}

	public static void setSavedStoreConfigs(List<StoreConfig> l) throws BackingStoreException, StoreConfigInvalidException {
		Preferences p = Preferences.userNodeForPackage(SavedStoreConfigs.class).
				node("savedStoreConfigs");
		p.clear();
		for (int i = 0; i < l.size(); i++) {
			p.put(string(i), l.get(i).toStringRepresentation());
		}
		p.flush();
	}

	public static List<Pair<StoreConfig, Preferences>> getOpenStores() throws BackingStoreException, StoreConfigInvalidException {
		Preferences p = Preferences.userNodeForPackage(SavedStoreConfigs.class).
				node("openStores");
		ArrayList<Pair<StoreConfig, Preferences>> l = new ArrayList<Pair<StoreConfig, Preferences>>();
		for (String name : p.childrenNames()) {
			Preferences n = p.node(name);
			l.add(p(new StoreConfig(n.get("config", null)), n.node("prefs")));
		}
		return l;
	}

	public static void setOpenStores(HashMap<StoreConfig, MainFrame> openStores) throws BackingStoreException, StoreConfigInvalidException, FatalStorageException {
		Preferences p = Preferences.userNodeForPackage(SavedStoreConfigs.class).
				node("openStores");
		p.clear();
		for (String cn : p.childrenNames()) {
			p.node(cn).removeNode();
		}
		int i = 0;
		for (Entry<StoreConfig, MainFrame> e : openStores.entrySet()) {
			Preferences n = p.node(string(i++));
			n.put("config", e.getKey().toStringRepresentation());
			e.getValue().writePrefs(n.node("prefs"));
		}
		p.flush();
	}
}
