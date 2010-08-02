package com.metalbeetle.fruitbat;

import com.metalbeetle.fruitbat.atrstorage.ATRStorageSystem;
import com.metalbeetle.fruitbat.gui.MainFrame;
import com.metalbeetle.fruitbat.gui.SplashWindow;
import com.metalbeetle.fruitbat.gui.setup.ConfigDialog;
import com.metalbeetle.fruitbat.gui.setup.ConfigPanel;
import com.metalbeetle.fruitbat.multiplexstorage.MultiplexStorageSystem;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.Store;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class Main {
	public static void main(String[] args) throws Exception {
		//ConfigDialog.newConfig(new MultiplexStorageSystem(), null);
		/*MultiplexStorageSystem mss = new MultiplexStorageSystem();
		Fruitbat f = new Fruitbat(mss.init(typedL(Object.class, typedL(Object.class,
				new StorageConfig(
					new ATRStorageSystem(),
					typedL(Object.class, new File("Documents 1"))),
				new StorageConfig(
					new ATRStorageSystem(),
					typedL(Object.class, new File("Documents 2")))
				)), new SplashWindow()));
		MainFrame mf = new MainFrame(f);
		mf.setLocationRelativeTo(null);
		mf.setVisible(true);*/
		new Fruitbat(); // Fly my pretties!
	}

	static void printlist(List<?> l) {
		System.out.println(Arrays.toString(l.toArray()));
	}
}
