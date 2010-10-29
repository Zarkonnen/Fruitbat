package com.metalbeetle.fruitbat.s3storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.io.InputStream;

/**
 * This class acts as a wrapper around selected S3 functionality, retrying several times in case of
 * failure. This is to counteract S3's tendency to randomly not work on occasion.
 */
class RetryingS3 {
	static final int FINAL_TIMEOUT = 12000;
	static final int INITIAL_WAIT = 100;
	static final int WAIT_INCREMENT = 1000;

	final AmazonS3 s3;

	RetryingS3(AmazonS3 s3) { this.s3 = s3; }

	void doze(int ms) throws FatalStorageException {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new FatalStorageException("S3 connection interrupted.", e);
		}
	}

	boolean doesBucketExist(String bucketName) throws FatalStorageException {
		//System.err.println("doesBucketExist");
		int wait = INITIAL_WAIT;
		Exception finalE = null;
		while (wait < FINAL_TIMEOUT) {
			try {
				return s3.doesBucketExist(bucketName);
			} catch (Exception e) {
				finalE = e;
				doze(wait += WAIT_INCREMENT);
			}
		}
		throw new FatalStorageException("Lost patience with S3 during doesBucketExist.", finalE);
	}
	
	void createBucket(String bucketName) throws FatalStorageException {
		//System.err.println("createBucket");
		int wait = INITIAL_WAIT;
		Exception finalE = null;
		while (wait < FINAL_TIMEOUT) {
			try {
				s3.createBucket(bucketName);
				return;
			} catch (Exception e) {
				finalE = e;
				doze(wait += WAIT_INCREMENT);
			}
		}
		throw new FatalStorageException("Lost patience with S3 during createBucket.", finalE);
	}

	ObjectListing listObjects(ListObjectsRequest lor) throws FatalStorageException {
		//System.err.println("listObjects");
		int wait = INITIAL_WAIT;
		Exception finalE = null;
		while (wait < FINAL_TIMEOUT) {
			try {
				ObjectListing ol = s3.listObjects(lor);
				if (ol == null) {
					throw new Exception("S3 returned null object listing.");
				}
				return ol;
			} catch (Exception e) {
				finalE = e;
				doze(wait += WAIT_INCREMENT);
			}
		}
		throw new FatalStorageException("Lost patience with S3 during listObjects.", finalE);
	}

	ObjectListing listNextBatchOfObjects(ObjectListing ol1) throws FatalStorageException {
		//System.err.println("listNextBatchOfObjects");
		int wait = INITIAL_WAIT;
		Exception finalE = null;
		while (wait < FINAL_TIMEOUT) {
			try {
				ObjectListing ol2 = s3.listNextBatchOfObjects(ol1);
				if (ol2 == null) {
					throw new Exception("S3 returned null object listing batch.");
				}
				return ol2;
			} catch (Exception e) {
				finalE = e;
				doze(wait += WAIT_INCREMENT);
			}
		}
		throw new FatalStorageException("Lost patience with S3 during listNextBatchOfObjects.",
				finalE);
	}

	void deleteObject(String bucketName, String key) throws FatalStorageException {
		//System.err.println("deleteObject");
		int wait = INITIAL_WAIT;
		Exception finalE = null;
		while (wait < FINAL_TIMEOUT) {
			try {
				s3.deleteObject(bucketName, key);
				return;
			} catch (Exception e) {
				finalE = e;
				doze(wait += WAIT_INCREMENT);
			}
		}
		throw new FatalStorageException("Lost patience with S3 during deleteObject.", finalE);
	}

	void putObject(String bucketName, String key, InputStream is, ObjectMetadata omd) throws FatalStorageException {
		//System.err.println("putObject");
		s3.putObject(bucketName, key, is, omd);
	}

	S3Object getObject(String bucketName, String key) throws FatalStorageException {
		//System.err.println("getObject");
		int wait = INITIAL_WAIT;
		Exception finalE = null;
		while (wait < FINAL_TIMEOUT) {
			try {
				S3Object o = s3.getObject(bucketName, key);
				if (o == null) {
					throw new Exception("S3 returned null object listing batch.");
				}
				return o;
			} catch (Exception e) {
				finalE = e;
				doze(wait += WAIT_INCREMENT);
			}
		}
		throw new FatalStorageException("Lost patience with S3 during putObject.", finalE);
	}
}
