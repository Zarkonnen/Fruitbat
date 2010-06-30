package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.csvstorage.CSVKeyIndex;
import com.metalbeetle.fruitbat.csvstorage.CSVStore;
import com.metalbeetle.fruitbat.storage.Document;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class Main {
	public static void main(String[] args) throws Exception {
		// Create tmp folder.
		File sF = File.createTempFile("FruitbatStore", "");
		sF.delete();
		sF.mkdirs();
		// Use it as a store.
		CSVStore s = new CSVStore(sF);
		Document d = s.create();
		d.put("foo", "bar");
		// Now create a new store instance aiming at the same location, as if we'd gone away for a
		// while.
		s = new CSVStore(sF);
		printlist(s.docs());
		System.out.println(s.docs().get(0).get("foo"));
		// Now create an index and do some searches.
		CSVKeyIndex index = new CSVKeyIndex(s);
		printlist(index.searchKeys(l("foo")).a);
		s.create().put("foo", "quux");
		printlist(index.searchKeys(l("foo")).a);
		// Now persist the index before recreating it, resulting in hopefully the same results.
		index.close();
		index = new CSVKeyIndex(s);
		printlist(index.searchKeys(l("foo")).a);
	}

	static void printlist(List<?> l) {
		System.out.println(Arrays.toString(l.toArray()));
	}
}
