package com.metalbeetle.fruitbat;

import java.io.File;
import java.util.List;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.auth.AWSCredentials;
import com.metalbeetle.fruitbat.filestorage.FileStore;
import com.metalbeetle.fruitbat.gui.DummyProgressMonitor;
import com.metalbeetle.fruitbat.hierarchicalstorage.HSIndex;
import com.metalbeetle.fruitbat.s3storage.S3StorageSystem;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import java.util.Properties;
import java.util.Random;
import static com.metalbeetle.fruitbat.util.Collections.*;

public final class TestStoreManagers {
	private TestStoreManagers() {}

	public static List<TestStoreManager> get() {
		return typedL(TestStoreManager.class, new FileSM(), new S3SM());
	}

	static class FileSM implements TestStoreManager {
		EnhancedStore s;
		HSIndex i;
		File f;

		public void setUp() throws FatalStorageException {
			s = new EnhancedStore(new FileStore(f = Util.createTempFolder(),
					new DummyProgressMonitor()));
			i = (HSIndex) s.getIndex();
		}

		public void reboot() throws FatalStorageException {
			s.close();
			s = new EnhancedStore(new FileStore(((FileStore) s.s).getFile(),
					new DummyProgressMonitor()));
			i = (HSIndex) s.getIndex();
		}

		public void crashAndReboot() throws FatalStorageException {
			s = new EnhancedStore(new FileStore(((FileStore) s.s).getFile(),
					new DummyProgressMonitor()));
			i = (HSIndex) s.getIndex();
		}

		public void tearDown() {
			Util.deleteRecursively(f);
		}

		public EnhancedStore getStore() { return s; }
		public HSIndex getIndex() { return i; }
	}

	static class S3SM implements TestStoreManager {
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

		EnhancedStore s;
		HSIndex i;
		String bucketName;

		public void setUp() throws Exception {
			bucketName = "fruitbattestbucket" + Math.abs(new Random().nextLong());
			crashAndReboot();
		}

		public void reboot() throws Exception {
			s.close();
			crashAndReboot();
		}

		public void crashAndReboot() throws Exception {
			StoreConfig sc1 = new StoreConfig(new S3StorageSystem(), typedL(Object.class,
					accessKey, secretKey, bucketName, /*password*/ "jamcat"));
			s = sc1.init(new DummyProgressMonitor());
			i = (HSIndex) s.getIndex();
		}

		public void tearDown() throws Exception {
			deleteBucket(bucketName);
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
					for (S3ObjectSummary os : ol.getObjectSummaries()) {
						s3.deleteObject(bucketName, os.getKey());
					}
					if (!ol.isTruncated()) { break; }
					ol = s3.listNextBatchOfObjects(ol);
				}
				s3.deleteBucket(bucketName);
			}
		}

		public EnhancedStore getStore() throws Exception { return s; }

		public HSIndex getIndex() throws Exception { return i; }
	}
}
