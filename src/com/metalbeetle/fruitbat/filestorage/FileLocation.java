package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.hierarchicalstorage.KVFile;
import com.metalbeetle.fruitbat.hierarchicalstorage.Location;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.io.LocalFile;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.util.Misc;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** A location pointing to a file on the local file system. */
public class FileLocation implements Location, LocalFile {
	final File f;

	public FileLocation(File f) { this.f = f; }

	public String getName() { return f.getName(); }

	public boolean exists() { return f.exists(); }

	public Location parent() throws FatalStorageException {
		File parent = f.getParentFile();
		return parent == null ? null : new FileLocation(parent);
	}

	public Location child(String name) throws FatalStorageException {
		return new FileLocation(new File(f, name));
	}

	public List<Location> children() throws FatalStorageException {
		ArrayList<Location> l = new ArrayList<Location>();
		for (File c : f.listFiles()) {
			l.add(new FileLocation(c));
		}
		return l;
	}

	public KVFile kvFile() throws FatalStorageException {
		return new AppendingKVFile(f);
	}

	public KVFile kvFile(Location cache, HashMap<String, String> defaults) throws FatalStorageException {
		return new AppendingKVFile(f, ((FileLocation) cache).f, defaults);
	}

	public void put(DataSrc data) throws FatalStorageException {
		try {
			Misc.srcToFile(data, f);
		} catch (IOException e) {
			throw new FatalStorageException("Could not save " + data.getName() + " to " + f + ".",
					e);
		}
	}

	public void delete() throws FatalStorageException {
		delete(f);
	}

	void delete(File f) {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		f.delete();
	}

	public CommittableOutputStream getOutputStream() {
		return new MyCOS();
	}

	public File getLocalFile() {
		return f;
	}

	class MyCOS implements CommittableOutputStream {
		public OutputStream stream() throws IOException {
			return new FileOutputStream(f);
		}

		public void commitIfNotAborted() throws IOException {
			// Does nothing.
		}

		public void abort() {
			// Does nothing.
		}

		public boolean isAbortable() { return false; }
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(f);
	}

	public long getLength() throws IOException {
		return f.length();
	}

	@Override
	public String toString() {
		return f.getPath();
	}
}
