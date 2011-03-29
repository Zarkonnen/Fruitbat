package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;

public interface KVFileManager {
	public void setUp(boolean cacheF) throws Exception;
	public void reboot() throws Exception;
	public void tearDown() throws Exception;
	public KVFile get() throws Exception;
}
