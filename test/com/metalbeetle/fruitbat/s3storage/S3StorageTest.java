package com.metalbeetle.fruitbat.s3storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class S3StorageTest {
	static String accessKey;
	static String secretKey;

	@Test
	public void testStoreCreation() throws FatalStorageException, StoreConfigInvalidException {
		String bucketName = "fruitbat-test-bucket-" + new Random().nextLong();
		StoreConfig sc1 = new StoreConfig(new S3StorageSystem(), typedL(Object.class,
				accessKey, secretKey, bucketName));
		Store s = sc1.init(new DummyProgressMonitor());
		s.close();
		deleteBucket(bucketName);
	}

	@BeforeClass
	public static void setup() throws IOException {
		Properties creds = new Properties();
		creds.load(S3StorageTest.class.getResourceAsStream("s3testcredentials.properties"));
		accessKey = creds.getProperty("accessKey");
		secretKey = creds.getProperty("secretKey");
	}

	void deleteBucket(String bucketName) {
		AmazonS3 s3 = new AmazonS3Client(new AWSCredentials() {
			public String getAWSAccessKeyId() {
				return accessKey;
			}

			public String getAWSSecretKey() {
				return secretKey;
			}
		});
		if (s3.doesBucketExist(bucketName)) {
			ObjectListing ol = s3.listObjects(bucketName);
			while (true) {
				for (S3ObjectSummary s : ol.getObjectSummaries()) {
					s3.deleteObject(bucketName, s.getKey());
				}
				if (!ol.isTruncated()) { break; }
				ol = s3.listNextBatchOfObjects(ol);
			}
			s3.deleteBucket(bucketName);
		}
	}
}