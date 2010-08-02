package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.storage.ConfigField;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import com.metalbeetle.fruitbat.storage.Utils;
import java.util.ArrayList;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class MultiplexStorageSystem implements StorageSystem {
	static final String CANNOT_OPEN_SLAVE = "CANNOT_OPEN_SLAVE";

	public String getDescription() {
		return "Stores documents in a master store as well as in one or several backup stores. " +
				"If a backup store is inaccessible you can continue working, and the backup will " +
				"be brought up to date the next time you open the multiplexed store.";
	}

	public List<ConfigField> getConfigFields() {
		return typedL(ConfigField.class, new MultiplexedStoresField("Stores"));
	}

	public Store init(List<Object> configValues, ProgressMonitor pm) throws FatalStorageException, StoreConfigInvalidException {
		Utils.checkConfigValues(new StoreConfig(this, configValues));
		List<StoreConfig> configs = (List<StoreConfig>)(List) configValues.get(0);
		try {
			ArrayList<Store> stores = new ArrayList<Store>();
			stores.add(configs.get(0).system.init(configs.get(0).configFieldValues, pm));
			for (int i = 1; i < configs.size(); i++) {
				try {
					stores.add(configs.get(i).system.init(configs.get(i).configFieldValues, pm));
				} catch (Exception e) {
					pm.showWarning(CANNOT_OPEN_SLAVE, "Unable to open backup store",
							"Unable to communicate with backup store. You can continue working, " +
							"and your changes will be pushed into the backup when communication " +
							"is restored.");
				}
			}
			return new MultiplexStore(stores, pm);
		} catch (Exception e) {
			throw new FatalStorageException("Unable to open master store.", e);
		}
	}

	@Override
	public String toString() { return "Multiplexed Storage"; }
}
