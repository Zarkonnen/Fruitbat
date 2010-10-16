package com.metalbeetle.fruitbat.s3storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import com.metalbeetle.fruitbat.hierarchicalstorage.Location;
import com.metalbeetle.fruitbat.io.ByteArraySrc;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class S3Location implements Location {
	final AmazonS3 s3;
	final String bucketName;
	final String path;
	boolean bucketExists = false;

	public S3Location(AmazonS3 s3, String bucketName, String path) {
		this.s3 = s3;
		this.bucketName = bucketName;
		this.path = path;
		ensureBucketExists();
	}

	private S3Location(AmazonS3 s3, String bucketName, String path, boolean bucketExists) {
		this.s3 = s3;
		this.bucketName = bucketName;
		this.path = path;
		this.bucketExists = bucketExists;
		ensureBucketExists();
	}

	public String getName() {
		if (path.contains("/")) {
			return path.substring(path.lastIndexOf("/") + 1);
		} else {
			return path;
		}
	}

	public String getPath() {
		return "s3://" + bucketName + "/" + path;
	}

	public boolean exists() {
		ensureBucketExists();
		try {
			return s3.getObjectMetadata(bucketName, path) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public Location parent() {
		ensureBucketExists();
		if (path.contains("/")) {
			return new S3Location(s3, bucketName, path.substring(0, path.lastIndexOf("/")),
					bucketExists);
		} else {
			if (path.isEmpty()) {
				return null;
			} else {
				return new S3Location(s3, bucketName, "", bucketExists);
			}
		}
	}

	public Location child(String name) {
		if (path.isEmpty()) {
			return new S3Location(s3, bucketName, name, bucketExists);
		} else {
			return new S3Location(s3, bucketName, path + "/" + name, bucketExists);
		}
	}

	public List<Location> children() {
		ensureBucketExists();
		ObjectListing ol = s3.listObjects(
				new ListObjectsRequest().
				withBucketName(bucketName).
				withPrefix(path + "/").
				withDelimiter("/"));
		ArrayList<Location> c = new ArrayList<Location>();
		while (true) {
			for (S3ObjectSummary os : ol.getObjectSummaries()) {
				c.add(new S3Location(s3, bucketName, os.getKey(), bucketExists));
			}
			if (!ol.isTruncated()) {
				break;
			} else {
				s3.listNextBatchOfObjects(ol);
			}
		}
		return c;
	}

	public KVFile kvFile() {
		ensureBucketExists();
		return new LocationKVFile(this, new HashMap<String, String>());
	}

	public KVFile kvFile(Location cache, HashMap<String, String> defaults) {
		ensureBucketExists();
		return new LocationKVFile(this, defaults);
	}

	public void put(DataSrc data) throws FatalStorageException {
		mkAncestors();
		try {
			s3.putObject(bucketName, path, data.getInputStream(), new ObjectMetadata());
		} catch (IOException e) {
			throw new FatalStorageException("Could not store data on S3.", e);
		}
	}

	public void delete() {
		s3.deleteObject(bucketName, path);
	}

	public CommittableOutputStream getOutputStream() {
		ensureBucketExists();
		return new MyCOS();
	}

	public InputStream getInputStream() throws IOException {
		ensureBucketExists();
		return s3.getObject(bucketName, path).getObjectContent();
	}

	void ensureBucketExists() {
		if (bucketExists) { return; }
		if (!s3.doesBucketExist(bucketName)) {
			s3.createBucket(bucketName);
			bucketExists = true;
		}
	}

	void mkAncestors() throws FatalStorageException {
		ensureBucketExists();
		S3Location p = this;
		while ((p = (S3Location) p.parent()) != null) {
			if (!p.exists()) {
				try {
					s3.putObject(bucketName, p.path, new ByteArrayInputStream(new byte[0]),
							new ObjectMetadata());
				} catch (Exception e) {
					throw new FatalStorageException("Could not create parent location " + p + ".",
							e);
				}
			}
		}
	}

	class MyCOS implements CommittableOutputStream {
		final ByteArrayOutputStream stream = new ByteArrayOutputStream();
		boolean aborted = false;

		public OutputStream stream() throws IOException { return stream; }

		public void commitIfNotAborted() throws IOException {
			if (aborted) { return; }
			try {
				put(new ByteArraySrc(stream.toByteArray(), "streamed data"));
			} catch (FatalStorageException e) {
				throw new IOException("Could not save data to " + getName() + ".", e);
			}
		}

		public void abort() {
			aborted = true;
			stream.reset();
		}
	}

	@Override
	public String toString() {
		return getPath();
	}
}
