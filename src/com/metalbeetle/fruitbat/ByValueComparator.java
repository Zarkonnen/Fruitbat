package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import static com.metalbeetle.fruitbat.util.Collections.*;

/**
 * Compares documents by looking at the values for the given keys. Assumes all documents compared
 * have all keys mentioned. Uses special ordering for dates and numbers.
 */
public final class ByValueComparator implements Comparator<Document> {
	public final List<String> cmpKeys;
	static final Pattern DATE_P = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d");
	static final Pattern NUM_P = Pattern.compile("\\d+([.]\\d+)?");

	public ByValueComparator(List<String> cmpKeys) {
		this.cmpKeys = immute(cmpKeys);
	}

	public int compare(Document d1, Document d2) {
		try {
			for (String k : cmpKeys) {
				//int cmp = d1.get(k).compareTo(d2.get(k));
				int cmp = compare(d1.get(k), d2.get(k));
				if (cmp != 0) { return cmp; }
			}

			// No difference!
			return 0;
		} catch (FatalStorageException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Special comparator function that treats dates and numbers differently.
	 */
	static int compare(String a, String b) {
		// If it's a date, have most recent first.
		if (DATE_P.matcher(a).matches() && DATE_P.matcher(b).matches()) {
			return b.compareTo(a);
		}
		// If it's a number, compare numerically.
		if (NUM_P.matcher(a).matches() && NUM_P.matcher(b).matches()) {
			double da = Double.parseDouble(a);
			double db = Double.parseDouble(b);
			return da > db ? 1 : db > da ? -1 : 0;
		}
		// It's just some string.
		return a.compareTo(b);
	}
}
