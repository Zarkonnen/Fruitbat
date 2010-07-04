package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.gui.MainFrame;
import com.metalbeetle.fruitbat.storage.Document;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Main {
	public static void main(String[] args) throws Exception {
		Fruitbat f = new Fruitbat(new File("Documents"));
		Document a = f.getStore().create();
		a.put("key", "value");
		MainFrame mf = new MainFrame(f);
		mf.setVisible(true);
		mf.setLocationRelativeTo(null);
	}

	static void printlist(List<?> l) {
		System.out.println(Arrays.toString(l.toArray()));
	}
}
