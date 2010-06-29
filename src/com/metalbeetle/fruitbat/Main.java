package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.Store;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Main {
	public static void main(String[] args) throws Exception {
		// Create tmp folder.
		File sF = File.createTempFile("FruitbatStore", "");
		sF.delete();
		sF.mkdirs();
		// Use it as a store.
		Store s = new Store(sF);
		Document d = s.create();
		d.put("foo", "bar");
		// Now create a new store instance aiming at the same location, as if we'd gone away for a
		// while.
		s = new Store(sF);
		printlist(s.docs());
		System.out.println(s.docs().get(0).get("foo"));
	}

	static void printlist(List<?> l) {
		System.out.println(Arrays.toString(l.toArray()));
	}
}
