package com.metalbeetle.fruitbat.s3storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.metalbeetle.fruitbat.TestStoreManagers;
import com.metalbeetle.fruitbat.io.StringSrc;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.Properties;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class S3LocationTest {
	static String accessKey;
	static String secretKey;
	static {
		Properties creds = new Properties();
		try {
			creds.load(TestStoreManagers.class.getResourceAsStream("s3testcredentials.properties"));
			accessKey = creds.getProperty("accessKey");
			secretKey = creds.getProperty("secretKey");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testNoS3LocationDuplicates() throws FatalStorageException {
		Random r = new Random();
		String bucket = r.nextInt() + "-s3testbucket";
		String pwd = "jamcat";
		AmazonS3 s3 = new AmazonS3Client(new AWSCredentials() {
			public String getAWSAccessKeyId() {
				return accessKey;
			}

			public String getAWSSecretKey() {
				return secretKey;
			}
		});
		try {
			S3Location.Factory f = new S3Location.Factory(bucket, s3, pwd);
			f.getLocation("a/b/c/1").put(new StringSrc("jam"));
			f.getLocation("a/b/c/2").put(new StringSrc("jam"));
			f.getLocation("a/b/c/3").put(new StringSrc("jam"));
			f = new S3Location.Factory(bucket, s3, pwd);
			assertEquals(1, f.getLocation("a").children().size());
			assertEquals(1, f.getLocation("a/b").children().size());
			assertEquals(3, f.getLocation("a/b/c").children().size());
			assertEquals(0, f.getLocation("a/b/foo").children().size());
			assertEquals(0, f.getLocation("a/b/c/1").children().size());
		} finally {
			while (s3.doesBucketExist(bucket)) {
				try {
					ObjectListing ol = s3.listObjects(bucket);
					while (true) {
						for (S3ObjectSummary os : ol.getObjectSummaries()) {
							s3.deleteObject(bucket, os.getKey());
						}
						if (!ol.isTruncated()) { break; }
						ol = s3.listNextBatchOfObjects(ol);
					}
					s3.deleteBucket(bucket);
				} catch (Exception e) {}
			}
		}
	}
}
