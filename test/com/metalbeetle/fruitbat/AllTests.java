/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.atrio.ATRTest;
import com.metalbeetle.fruitbat.atrio.ATRWriterTest;
import com.metalbeetle.fruitbat.atrio.SimpleReaderTest;
import com.metalbeetle.fruitbat.filestorage.DataStorageTest;
import com.metalbeetle.fruitbat.filestorage.FileStorageTest;
import com.metalbeetle.fruitbat.filestorage.DocIndexTest;
import com.metalbeetle.fruitbat.filestorage.KVFileTest;
import com.metalbeetle.fruitbat.filestorage.NecromancyTest;
import com.metalbeetle.fruitbat.io.CryptoTest;
import com.metalbeetle.fruitbat.multiplexstorage.MultiplexTest;
import com.metalbeetle.fruitbat.multiplexstorage.SyncTest;
import com.metalbeetle.fruitbat.s3storage.S3StorageTest;
import com.metalbeetle.fruitbat.storage.StoreConfigTest;
import com.metalbeetle.fruitbat.util.MiscTest;
import com.metalbeetle.fruitbat.util.PrefixBTreeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author zar
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CryptoTest.class,
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
	StoreConfigTest.class,
	S3StorageTest.class})
public class AllTests {}