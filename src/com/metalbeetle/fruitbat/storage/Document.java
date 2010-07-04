package com.metalbeetle.fruitbat.storage;

import java.io.File;
import java.net.URI;
import java.util.List;

/** Stores key/value string data and pages as files. */
public interface Document {
	public String getID();

	public boolean has(String key);
	public String get(String key);
	public void put(String key, String value);
	public void remove(String key);
	public void move(String srcKey, String dstKey);
	public List<String> keys();

	public boolean hasPage(String key);
	public URI getPage(String key);
	public List<String> pageKeys();
	public void putPage(String key, File f);
	public void removePage(String key);
	public void movePage(String srcKey, String dstKey);
}
