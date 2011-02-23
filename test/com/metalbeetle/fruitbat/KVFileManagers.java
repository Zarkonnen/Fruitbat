package com.metalbeetle.fruitbat;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.util.Properties;
import java.util.Map;
import java.util.Collections;
import com.metalbeetle.fruitbat.filestorage.AppendingKVFile;
import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import com.metalbeetle.fruitbat.s3storage.S3Location;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.util.Random;
import static com.metalbeetle.fruitbat.util.Collections.*;

public final class KVFileManagers {
	private KVFileManagers() {}

	public static List<KVFileManager> get() {
		return typedL(KVFileManager.class,/* new AppendKVFM(),*/ new S3KVFM());
	}

	static class AppendKVFM implements KVFileManager {
		File f;
		File cf;
		Map<String, String> defaults;
		AppendingKVFile kvf;

		public void setUp(boolean cacheF) throws Exception {
			setUp(cacheF, Collections.<String, String>emptyMap());
		}

		public void setUp(boolean cacheF, Map<String, String> defaults) throws Exception {
			this.defaults = defaults;
			f = File.createTempFile("kvfiletest", ".atr");
			if (cacheF) {
				cf = File.createTempFile("kvfiletest", ".atr");
			}
			reboot();
		}

		public void reboot() throws Exception {
			kvf = new AppendingKVFile(f, cf, defaults);
		}

		public void tearDown() throws Exception {
			f.delete();
			if (cf != null) { cf.delete(); }
		}

		public KVFile get() throws Exception {
			return kvf;
		}
	}

	static class S3KVFM implements KVFileManager {
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

		AmazonS3 s3;
		Map<String, String> defaults;
		String bucketName;
		KVFile kvf;
		S3Location s3l;
		final String fileName = "kvfile.atr";

		public void setUp(boolean cacheF) throws Exception {
			setUp(cacheF, Collections.<String, String>emptyMap());
		}

		public void setUp(boolean cacheF, Map<String, String> defaults) throws Exception {
			this.defaults = defaults;
			s3 = new AmazonS3Client(new AWSCredentials() {
				public String getAWSAccessKeyId() {
					return accessKey;
				}

				public String getAWSSecretKey() {
					return secretKey;
				}
			});
			bucketName = "s3kvfiletest-" + new Random().nextLong();
			s3l = new S3Location.Factory(bucketName, s3, "jamcat").getLocation(fileName);
			reboot();
		}

		public void reboot() throws Exception {
			kvf = s3l.kvFile(null, new HashMap<String, String>(defaults));
		}

		public void tearDown() throws Exception {
			deleteBucket(bucketName);
		}

		void deleteBucket(String bucketName) {
			// Code to truly delete bucket despite inconsistencies from S3.
			while (s3.doesBucketExist(bucketName)) {
				try {
					ObjectListing ol = s3.listObjects(bucketName);
					while (true) {
						for (S3ObjectSummary os : ol.getObjectSummaries()) {
							s3.deleteObject(bucketName, os.getKey());
						}
						if (!ol.isTruncated()) { break; }
						ol = s3.listNextBatchOfObjects(ol);
					}
					s3.deleteBucket(bucketName);
				} catch (Exception e) {}
			}
		}

		public KVFile get() throws Exception {
			return kvf;
		}
	}
}
