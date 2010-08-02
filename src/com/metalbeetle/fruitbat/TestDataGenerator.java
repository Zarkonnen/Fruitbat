package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.Store;
import java.util.Random;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class TestDataGenerator {
	public static void generate(Store s, int quantity, int year) {
		try {
			for (int i = 0; i < quantity; i++) {
				Document d = s.create();
				Random r = new Random();

				d.change(l(
						DataChange.put(new String[] { "bank", "water", "electricity", "gas", "phone", "insurance" }[r.nextInt(6)], ""),
						DataChange.put(new String[] { "bill", "letter", "notes" }[r.nextInt(3)], ""),
						DataChange.put(new String[] { "bob", "suzy", "mike", "anna", "alexandra", "susan", "phil"}[r.nextInt(7)], ""),
						DataChange.put(Fruitbat.DATE_KEY,
							year + "-" +
							"0" + (1 + r.nextInt(8)) + "-" +
							(10 + r.nextInt(18)))));
				if (d.has("bill")) {
					d.change(l(
							DataChange.put("cost", "" + r.nextInt(1000)),
							DataChange.put(new String[] { "topay", "paid" }[r.nextInt(2)], "")));
				}
			}
		} catch (Exception e) {}
	}
}
