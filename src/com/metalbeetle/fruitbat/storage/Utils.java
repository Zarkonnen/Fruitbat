package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.atrstorage.ATRStorageSystem;
import com.metalbeetle.fruitbat.multiplexstorage.MultiplexStorageSystem;
import com.metalbeetle.fruitbat.s3storage.S3StorageSystem;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

public final class Utils {
	private Utils() {}

	public static List<StorageSystem> getAvailableStorageSystems() {
		// TODO: Implement dynamic loading of plugins.
		return typedL(StorageSystem.class,
				new ATRStorageSystem(),
				new S3StorageSystem(),
				new MultiplexStorageSystem());
	}

	public static void checkConfigValues(StoreConfig c)
			throws StoreConfigInvalidException
	{
		List<Object> configValues = c.configFieldValues;
		List<ConfigField> fields = c.system.getConfigFields();
		String badStuff = "";
		int i = 0;
		while (i < configValues.size() && i < fields.size()) {
			Object v = configValues.get(i);
			ConfigField f = fields.get(i);
			if (v == null) {
				badStuff += "Please supply a value for " + f.getName() + ".\n";
			} else {
				if (!f.getExpectedValueClass().isAssignableFrom(v.getClass())) {
					badStuff += "Field " + f.getName() + ": Incompatible field value supplied. " +
							"Expected a " + f.getExpectedValueClass().getSimpleName() + ", but " +
							"got a " + v.getClass().getSimpleName() + ".\n";
				} else {
					String validError = f.validate(v);
					if (validError != null) {
						badStuff += "Field " + f.getName() + ": " + validError + "\n";
					}
				}
			}
			i++;
		}

		if (configValues.size() > fields.size()) {
			badStuff += (configValues.size() - fields.size()) + " configuration value(s) too " +
					"many supplied.";
		}
		if (configValues.size() < fields.size()) {
			badStuff += "Insufficient number of configuration values. Expected " + fields.size() +
					" values, but got only " + configValues.size() + ".";
		}

		if (badStuff.length() > 0) {
			throw new StoreConfigInvalidException(badStuff);
		}
	}
}
