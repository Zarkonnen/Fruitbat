package com.metalbeetle.fruitbat.prefs;

import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import static com.metalbeetle.fruitbat.util.Misc.*;

public final class SavedStoreConfigs {
	private SavedStoreConfigs() {}

	public static List<StoreConfig> getSavedStoreConfigs() throws BackingStoreException, StoreConfigInvalidException {
		Preferences p = Preferences.userNodeForPackage(SavedStoreConfigs.class).
				node("savedStoreConfigs");
		ArrayList<StoreConfig> l = new ArrayList<StoreConfig>();
		for (String name : p.keys()) {
			l.add(new StoreConfig(p.get(name, null)));
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
	}
}
