package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.gui.MainFrame;
import com.metalbeetle.fruitbat.storage.Document;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {
	public static void main(String[] args) throws Exception {
		Fruitbat f = new Fruitbat(new File("Documents"));
		/*for (int i = 0; i < 1000; i++) {
			Document d = f.getStore().create();
			Random r = new Random();

			d.put(new String[] { "bill", "letter", "topay", "notes" }[r.nextInt(3)], "");
			d.put("name", new String[] {"bob", "suzy", "mike"}[r.nextInt(3)]);
			d.put("d",
					(2005 + r.nextInt(6)) + "-" +
					"0" + (1 + r.nextInt(8)) + "-" +
					(10 + r.nextInt(18)));
		}*/
		MainFrame mf = new MainFrame(f);
		mf.setVisible(true);
		mf.setLocationRelativeTo(null);
	}

	static void printlist(List<?> l) {
		System.out.println(Arrays.toString(l.toArray()));
	}
}
