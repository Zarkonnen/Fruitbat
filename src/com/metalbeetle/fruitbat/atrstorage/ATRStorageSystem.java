package com.metalbeetle.fruitbat.atrstorage;

import com.metalbeetle.fruitbat.storage.ConfigField;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import com.metalbeetle.fruitbat.storage.Utils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class ATRStorageSystem implements StorageSystem {
	static final List<ConfigField> CONFIG_FIELDS;
	static {
		ArrayList<ConfigField> l = new ArrayList<ConfigField>();
		l.add(new ConfigField.FileField("Location", "Folders") {
			public String validate(File input) {
				if (input == null) { return "Please select a location"; }
				return input.isDirectory() ? null : "Location must be a folder";
			}

			@Override
			public boolean lookingForFolders() { return true; }
		});
		CONFIG_FIELDS = immute(l);
	}

	@Override
	public String toString() { return "Local file system storage"; }

	public String getDescription() {
		return "Stores documents on your local file system in a specific folder.";
	}

	public List<ConfigField> getConfigFields() { return CONFIG_FIELDS; }

	public Store init(List<Object> configValues, ProgressMonitor pm)
			throws FatalStorageException, StoreConfigInvalidException
	{
		Utils.checkConfigValues(new StoreConfig(this, configValues));
		return new ATRStore((File) configValues.get(0), pm);
	}
}
