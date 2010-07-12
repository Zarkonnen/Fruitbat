package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.gui.Dialogs;
import com.metalbeetle.fruitbat.gui.MainFrame;
import com.metalbeetle.fruitbat.storage.Document;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {
	public static void main(String[] args) throws Exception {
		Fruitbat f = new Fruitbat(new File("Documents"), new Dialogs());
		/*for (int i = 0; i < 100000; i++) {
			Document d = f.getStore().create();
			Random r = new Random();

			d.put(new String[] { "bank", "water", "electricity", "gas", "phone", "insurance" }[r.nextInt(6)], "");
			d.put(new String[] { "bill", "letter", "topay", "notes" }[r.nextInt(4)], "");
			d.put(new String[] {"bob", "suzy", "mike", "anna", "alexandra", "susan", "phil"}[r.nextInt(7)], "");
			d.put("d",
					(1990 + r.nextInt(20)) + "-" +
					"0" + (1 + r.nextInt(8)) + "-" +
					(10 + r.nextInt(18)));
		}*/
		MainFrame mf = new MainFrame(f);
		mf.setLocationRelativeTo(null);
		mf.setVisible(true);
	}

	static void printlist(List<?> l) {
		System.out.println(Arrays.toString(l.toArray()));
	}
}
