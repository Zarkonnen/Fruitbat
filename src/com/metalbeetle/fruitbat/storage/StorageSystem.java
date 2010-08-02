package com.metalbeetle.fruitbat.storage;

import java.util.List;

public interface StorageSystem {
	@Override
	public String toString();
	/** @return A HTML string describing the system. */
	public String getDescription();
	public List<ConfigField> getConfigFields();
	public Store init(List<Object> configValues, ProgressMonitor pm) throws FatalStorageException,
			StoreConfigInvalidException;
}
