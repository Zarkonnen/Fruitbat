package com.metalbeetle.fruitbat.s3storage;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.metalbeetle.fruitbat.fulltext.FullTextIndex;
import com.metalbeetle.fruitbat.hierarchicalstorage.HSStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.ProgressMonitor;

public class S3Store extends HSStore {
	public static final String BUCKET_NAME_PREFIX = "fruitbat-bucket-";

	public S3Store(final String accessKey, final String secretKey, String bucketName, String password, ProgressMonitor pm) throws FatalStorageException {
		super(
			new S3Location.Factory(
				BUCKET_NAME_PREFIX + bucketName,
				new AmazonS3Client(new MyCredentials(accessKey, secretKey),
					new ClientConfiguration().
						withConnectionTimeout(200000).
						withMaxErrorRetry(10)),
				password
			).getLocation(""),
			pm
		);
	}

	static class MyCredentials implements AWSCredentials {
		final String accessKey; final String secretKey;

		MyCredentials(String accessKey, String secretKey) {
			this.accessKey = accessKey;
			this.secretKey = secretKey;
		}

		public String getAWSAccessKeyId() { return accessKey; }
		public String getAWSSecretKey() { return secretKey; }
	}

	public FullTextIndex getFullTextIndex() { return null; }
}
