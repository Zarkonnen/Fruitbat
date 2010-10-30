package com.metalbeetle.fruitbat.s3storage;

import com.metalbeetle.fruitbat.storage.Utils;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import java.util.ArrayList;
import com.metalbeetle.fruitbat.storage.ConfigField;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class S3StorageSystem implements StorageSystem {
	static final List<ConfigField> CONFIG_FIELDS;
	static {
		ArrayList<ConfigField> l = new ArrayList<ConfigField>();
		l.add(new ConfigField.NonEmptyStringField("Access Key"));
		l.add(new ConfigField.NonEmptyStringField("Secret Key"));
		l.add(new ConfigField.RegexStringField("Bucket Name", "[a-z0-0]+", "Lowercase letters and numbers only"));
		l.add(new ConfigField.NonEmptyStringField("Encryption Password"));
		CONFIG_FIELDS = immute(l);
	}

	public String getDescription() {
		return "Stores files in an Amazon S3 bucket.";
	}

	@Override
	public String toString() { return "S3 Storage"; }

	public List<ConfigField> getConfigFields() {
		return CONFIG_FIELDS;
	}

	public Store init(List<Object> configValues, ProgressMonitor pm) throws FatalStorageException, StoreConfigInvalidException {
		Utils.checkConfigValues(new StoreConfig(this, configValues));
		return new S3Store(
				(String) configValues.get(0),
				(String) configValues.get(1),
				(String) configValues.get(2),
				(String) configValues.get(3),
				pm);
	}

	@Override
	public boolean equals(Object o2) {
		return o2 instanceof S3StorageSystem;
	}

	@Override
	public int hashCode() { return -998; }
}
