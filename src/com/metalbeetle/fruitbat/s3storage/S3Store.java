package com.metalbeetle.fruitbat.s3storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.metalbeetle.fruitbat.fulltext.FullTextIndex;
import com.metalbeetle.fruitbat.hierarchicalstorage.HSStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;

public class S3Store extends HSStore {

	public S3Store(final String accessKey, final String secretKey, String bucketName, ProgressMonitor pm) throws FatalStorageException {
		super(new S3Location(new AmazonS3Client(
				new AWSCredentials() {
					public String getAWSAccessKeyId() { return accessKey; }
					public String getAWSSecretKey() { return secretKey; }
				}),
				bucketName,
				/* path */ ""),
		pm);
	}

	public FullTextIndex getFullTextIndex() {
		return null;
	}
}
