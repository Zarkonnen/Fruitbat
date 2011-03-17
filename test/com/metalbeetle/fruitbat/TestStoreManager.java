package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.EnhancedStore;

public interface TestStoreManager {
	public void setUp() throws Exception;
	public void reboot() throws Exception;
	public void crashAndReboot() throws Exception;
	public void tearDown() throws Exception;
	public EnhancedStore getStore() throws Exception;
	public DocIndex getIndex() throws Exception;
}
