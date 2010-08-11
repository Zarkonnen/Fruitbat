/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.atrio.ATRTest;
import com.metalbeetle.fruitbat.atrio.ATRWriterTest;
import com.metalbeetle.fruitbat.atrio.SimpleReaderTest;
import com.metalbeetle.fruitbat.atrstorage.DataStorageTest;
import com.metalbeetle.fruitbat.atrstorage.FileStorageTest;
import com.metalbeetle.fruitbat.atrstorage.DocIndexTest;
import com.metalbeetle.fruitbat.atrstorage.KVFileTest;
import com.metalbeetle.fruitbat.atrstorage.NecromancyTest;
import com.metalbeetle.fruitbat.multiplexstorage.MultiplexTest;
import com.metalbeetle.fruitbat.multiplexstorage.SyncTest;
import com.metalbeetle.fruitbat.storage.StoreConfigTest;
import com.metalbeetle.fruitbat.util.MiscTest;
import com.metalbeetle.fruitbat.util.PrefixBTreeTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author zar
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ATRTest.class,
	ATRWriterTest.class,
	SimpleReaderTest.class,
	KVFileTest.class,
	DataStorageTest.class,
	FileStorageTest.class,
	DocIndexTest.class,
	PrefixBTreeTest.class,
	NecromancyTest.class,
	MiscTest.class,
	MultiplexTest.class,
	SyncTest.class,
	StoreConfigTest.class})
public class AllTests {

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

}