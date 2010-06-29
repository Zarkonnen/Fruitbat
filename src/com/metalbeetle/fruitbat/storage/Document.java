package com.metalbeetle.fruitbat.storage;
import static com.metalbeetle.fruitbat.util.Misc.*;

import java.io.File;
import java.util.List;

public class Document {
	final File location;
	final KVFile data;
	final KVFile files;

	public Document(File location) {
		this.location = location;
		data = new KVFile(new File(location, "data.csv"));
		files = new KVFile(new File(location, "files.csv"));
	}

	public String getID() { return location.getName(); }

	public String get(String key) { return data.get(key); }
	public void put(String key, String value) { data.put(key, value); }
	public boolean has(String key) { return data.has(key); }
	public List<String> keys() { return data.keys(); }

	public File getPage(String key) { return new File(location, files.get(key)); }

	public void putPage(String key, File f) {
		String name = findSlot(f);
		File newF = new File(location, name);
		mkAncestors(newF);
		if (!f.renameTo(newF)) {
			throw new RuntimeException("Couldn't store page at " + key + ".\n" +
					"Can't move " + f + " to " + newF + ".");
		}
		files.put(key, name);
	}

	private String findSlot(File f) {
		String name = f.getName();
		int i = 2;
		int dotIndex = name.lastIndexOf(".");
		String preDot  = dotIndex == -1 ? name : name.substring(0, dotIndex);
		String postDot = dotIndex == -1 ? ""   : name.substring(dotIndex);
		while (new File(location, name).exists()) {
			name = preDot + "_" + i + postDot;
		}
		return name;
	}

	public boolean hasPage(String key) { return files.has(key); }
	public List<String> pageKeys() { return files.keys(); }

	@Override
	public String toString() {
		return "doc@" + location;
	}
}
