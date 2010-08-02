package com.metalbeetle.fruitbat.storage;

import java.io.File;

public abstract class PageChange implements Change {
	public static final class Put extends PageChange {
		public final String key;
		public final File value;
		Put(String key, File value) { this.key = key; this.value = value; }
	}
	public static final class Move extends PageChange {
		public final String srcKey;
		public final String dstKey;
		Move(String srcKey, String dstKey) { this.srcKey = srcKey; this.dstKey = dstKey; }
	}
	public static final class Remove extends PageChange {
		public final String key;
		Remove(String key) { this.key = key; }
	}

	public static Change put(String key, File value) {
		return new Put(key, value);
	}
	public static Change move(String srcKey, String dstKey) {
		return new Move(srcKey, dstKey);
	}
	public static Change remove(String key) {
		return new Remove(key);
	}
}
