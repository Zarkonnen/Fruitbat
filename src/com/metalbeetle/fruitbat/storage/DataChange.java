package com.metalbeetle.fruitbat.storage;

public abstract class DataChange implements Change {
	public static final class Put extends DataChange {
		public final String key;
		public final String value;
		Put(String key, String value) { this.key = key; this.value = value; }
	}
	public static final class Move extends DataChange {
		public final String srcKey;
		public final String dstKey;
		Move(String srcKey, String dstKey) { this.srcKey = srcKey; this.dstKey = dstKey; }
	}
	public static final class Remove extends DataChange {
		public final String key;
		Remove(String key) { this.key = key; }
	}

	public static DataChange put(String key, String value) {
		return new Put(key, value);
	}
	public static DataChange move(String srcKey, String dstKey) {
		return new Move(srcKey, dstKey);
	}
	public static DataChange remove(String key) {
		return new Remove(key);
	}
}
