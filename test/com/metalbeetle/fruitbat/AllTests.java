/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.atrio.ATRTest;
import com.metalbeetle.fruitbat.atrio.ATRWriterTest;
import com.metalbeetle.fruitbat.atrio.SimpleReaderTest;
import com.metalbeetle.fruitbat.storage.DataStorageTest;
import com.metalbeetle.fruitbat.storage.PageStorageTest;
import com.metalbeetle.fruitbat.storage.DocIndexTest;
import com.metalbeetle.fruitbat.filestorage.KVFileTest;
import com.metalbeetle.fruitbat.filestorage.ReliabilityTest;
import com.metalbeetle.fruitbat.storage.NecromancyTest;
import com.metalbeetle.fruitbat.io.CryptoTest;
import com.metalbeetle.fruitbat.multiplexstorage.MultiplexTest;
import com.metalbeetle.fruitbat.multiplexstorage.Multiplex2Test;
import com.metalbeetle.fruitbat.multiplexstorage.SyncTest;
import com.metalbeetle.fruitbat.s3storage.S3LocationTest;
import com.metalbeetle.fruitbat.storage.FullTextTest;
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
	S3LocationTest.class,
	KVFileTest.class,
	ReliabilityTest.class,
	DataStorageTest.class,
	PageStorageTest.class,
	DocIndexTest.class,
	PrefixBTreeTest.class,
	NecromancyTest.class,
	FullTextTest.class,
	MiscTest.class,
	MultiplexTest.class,
	Multiplex2Test.class,
	SyncTest.class,
	StoreConfigTest.class})
public class AllTests {}