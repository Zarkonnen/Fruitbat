package com.metalbeetle.fruitbat.s3storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import com.metalbeetle.fruitbat.hierarchicalstorage.Location;
import com.metalbeetle.fruitbat.io.Crypto.DecryptingInputStream;
import com.metalbeetle.fruitbat.io.Crypto.EncryptingOutputStream;
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
	static final String PATH_DELIMITER = "/";

	public static class Factory {
		final String bucketName;
		final String password;
		final RetryingS3 s3;
		final HashMap<String, S3Location> mapping = new HashMap<String, S3Location>(1024);

		public Factory(String bucketName, AmazonS3 amazonS3, String password) throws FatalStorageException {
			this.bucketName = bucketName;
			this.password = password;
			this.s3 = new RetryingS3(amazonS3);
			if (!s3.doesBucketExist(bucketName)) {
				s3.createBucket(bucketName);
			}
			ListObjectsRequest lor = new ListObjectsRequest().
					withBucketName(bucketName).
					withMaxKeys(32768);
			ObjectListing ol = s3.listObjects(lor);
			while (true) {
				for (S3ObjectSummary os : ol.getObjectSummaries()) {
					S3Location child = null;
					String path = os.getKey();
					do {
						S3Location loc;
						if (mapping.containsKey(path)) {
							loc = mapping.get(path);
						} else {
							loc = new S3Location(path, this);
							mapping.put(path, loc);
						}
						if (child != null) {
							loc.children.add(child);
						}
						child = loc;
					} while ((path = parentPath(path)) != null);
				}
				if (!ol.isTruncated()) { break; }
				ol = s3.listNextBatchOfObjects(ol);
			}
		}

		public S3Location getLocation(String path) {
			if (mapping.containsKey(path)) {
				return mapping.get(path);
			}
			return new S3Location(path, this);
		}

		void create(String path) {
			if (!mapping.containsKey(path)) {
				S3Location loc = new S3Location(path, this);
				mapping.put(path, loc);
				String parentPath = parentPath(path);
				if (parentPath != null) {
					create(parentPath);
					getLocation(parentPath).children.add(loc);
				}
			}
		}

		void delete(String path) throws FatalStorageException {
			if (mapping.containsKey(path)) {
				for (S3Location c : mapping.get(path).children) {
					delete(c.path);
				}
				mapping.remove(path);
				s3.deleteObject(bucketName, path);
			}
		}
	}

	static String parentPath(String path) {
		if (path.length() == 0) { return null; }
		if (path.contains(PATH_DELIMITER)) {
			return path.substring(0, path.lastIndexOf(PATH_DELIMITER));
		} else {
			return "";
		}
	}

	static String childPath(String parentPath, String name) {
		return parentPath.length() == 0 ? name : parentPath + "/" + name;
	}

	final String path;
	final Factory f;
	final ArrayList<S3Location> children = new ArrayList<S3Location>();

	private S3Location(String path, Factory f) {
		this.path = path;
		this.f = f;
	}

	public String getName() {
		return
				path.contains(PATH_DELIMITER)
				? path.substring(path.indexOf(PATH_DELIMITER) + 1)
				: path;
	}

	public long getLength() throws IOException {
		try {
			return f.s3.getObject(f.bucketName, path).getObjectMetadata().getContentLength();
		} catch (FatalStorageException e) {
			throw new IOException(e.getMessage());
		}
	}

	public boolean exists() throws FatalStorageException {
		return f.mapping.containsKey(path);
	}

	public Location parent() throws FatalStorageException {
		String parentPath = parentPath(path);
		return parentPath == null ? null : f.getLocation(path);
	}

	public Location child(String name) throws FatalStorageException {
		return f.getLocation(childPath(path, name));
	}

	public List<Location> children() throws FatalStorageException {
		return new ArrayList<Location>(f.getLocation(path).children);
	}

	public KVFile kvFile() {
		return new LocationKVFile(this, new HashMap<String, String>());
	}

	public KVFile kvFile(Location cache, HashMap<String, String> defaults) {
		return new LocationKVFile(this, defaults);
	}

	public void put(DataSrc data) throws FatalStorageException {
		f.create(path);
		try {
			// qqDPS This is realllllly inefficient!
			CommittableOutputStream cos = getOutputStream();
			int amt = 0;
			InputStream in = data.getInputStream();
			byte[] buf = new byte[2048];
			while ((amt = in.read(buf)) != -1) {
				cos.stream().write(buf, 0, amt);
			}
			cos.stream().close();
			cos.commitIfNotAborted();
			/*
			ObjectMetadata omd = new ObjectMetadata();
			if (data.getLength() != DataSrc.UNKNOWN_DATA_LENGTH) {
				omd.setContentLength(data.getLength());
			}
			f.s3.putObject(f.bucketName, path, data.getInputStream(), omd);
			 */
		} catch (IOException e) {
			throw new FatalStorageException("Could not store data on S3.", e);
		}
	}

	public void delete() throws FatalStorageException {
		f.delete(path);
	}

	public CommittableOutputStream getOutputStream() throws IOException {
		return new MyCOS();
	}

	public InputStream getInputStream() throws IOException {
		try {
			return new DecryptingInputStream(f.s3.getObject(f.bucketName, path).getObjectContent(),
					f.password);
		} catch (FatalStorageException e) {
			throw new IOException(e.getMessage());
		}
	}

	class MyCOS implements CommittableOutputStream {
		final ByteArrayOutputStream stream;
		final EncryptingOutputStream cryptoStream;
		boolean aborted = false;

		MyCOS() throws IOException {
			stream = new ByteArrayOutputStream();
			cryptoStream = new EncryptingOutputStream(stream, f.password);
		}

		public OutputStream stream() throws IOException { return cryptoStream; }

		public void commitIfNotAborted() throws IOException {
			if (aborted) { return; }
			try {
				// qqDPS
				//put(new ByteArraySrc(stream.toByteArray(), "streamed data"));
				f.create(path);
				ObjectMetadata omd = new ObjectMetadata();
				byte[] data = stream.toByteArray();
				omd.setContentLength(data.length);
				f.s3.putObject(f.bucketName, path, new ByteArrayInputStream(data), omd);
			} catch (FatalStorageException e) {
				throw new IOException("Could not save data to " + getName() + ".\n" +
						e.getMessage());
			}
		}

		public void abort() {
			aborted = true;
			stream.reset();
		}

		public boolean isAbortable() { return true; }
	}

	@Override
	public String toString() {
		return "s3://" + f.bucketName + "/" + path;
	}
}
