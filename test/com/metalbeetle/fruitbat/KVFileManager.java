package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import java.util.Map;

public interface KVFileManager {
	public void setUp(boolean cacheF) throws Exception;
	public void setUp(boolean cacheF, Map<String, String> defaults) throws Exception;
	public void reboot() throws Exception;
	public void tearDown() throws Exception;
	public KVFile get() throws Exception;
}
