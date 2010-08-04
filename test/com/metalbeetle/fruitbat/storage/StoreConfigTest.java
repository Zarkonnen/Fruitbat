package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.atrstorage.ATRStorageSystem;
import com.metalbeetle.fruitbat.multiplexstorage.MultiplexStorageSystem;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class StoreConfigTest {
	@Test
	public void testStringRepresentation() throws StoreConfigInvalidException {
		StoreConfig sc1 = new StoreConfig(new ATRStorageSystem(),
				typedL(Object.class, new File("").getAbsoluteFile()));
		assertEquals(sc1, new StoreConfig(sc1.toStringRepresentation()));
		StoreConfig sc2 = new StoreConfig(new ATRStorageSystem(),
				typedL(Object.class, new File("").getAbsoluteFile()));
		StoreConfig msc = new StoreConfig(new MultiplexStorageSystem(),
				typedL(Object.class, l(sc1, sc2)));
		assertEquals(msc, new StoreConfig(msc.toStringRepresentation()));
	}
}