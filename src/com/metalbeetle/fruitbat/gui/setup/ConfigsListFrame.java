package com.metalbeetle.fruitbat.gui.setup;

import com.metalbeetle.fruitbat.BlockingTask;
import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.prefs.SavedStoreConfigs;
import com.metalbeetle.fruitbat.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.Utils;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ConfigsListFrame extends JFrame {
	final Fruitbat app;
	final ProgressMonitor pm;

	final JScrollPane configsListSP;
		final JList configsList;
			final ConfigsListModel configsListM;
				final ArrayList<StoreConfig> configs = new ArrayList<StoreConfig>();
	final JPanel buttonP;
		final JButton openB;
		final JButton addB;
		final JButton editB;
		final JButton removeB;

	public ConfigsListFrame(final Fruitbat app, ProgressMonitor pm) {
		super("Fruitbat: Manage Stores");
		this.app = app;
		this.pm = pm;

		try {
			configs.addAll(SavedStoreConfigs.getSavedStoreConfigs(pm));
		} catch (Exception e) {
			pm.handleException(new Exception("Cannot load list of saved stores from preferences.",
					e), null);
		}

		setLayout(new BorderLayout());
		add(configsListSP = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
			configsListSP.setViewportView(configsList = new JList(configsListM = new ConfigsListModel(this)));
				configsList.addListSelectionListener(new ListSelectionListener() { public void valueChanged(ListSelectionEvent e) {
					removeB.setEnabled(configsList.getSelectedIndex() != -1);
					editB.setEnabled(configsList.getSelectedIndex() != -1);
					openB.setEnabled(configsList.getSelectedIndex() != -1);
				}});
				configsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				configsList.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() > 1) { open(); }
					}
				});
		add(buttonP = new JPanel(), BorderLayout.EAST);
			buttonP.setLayout(new BoxLayout(buttonP, BoxLayout.Y_AXIS));
			buttonP.add(openB = new JButton("Open"));
				getRootPane().setDefaultButton(openB);
				openB.setEnabled(false);
				openB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					open();
				}});
			buttonP.add(addB = new JButton("Add"));
				addB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					add();
				}});
			buttonP.add(editB = new JButton("Edit"));
				editB.setEnabled(false);
				editB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					edit();
				}});
			buttonP.add(removeB = new JButton("Remove"));
				removeB.setEnabled(false);
				removeB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					configs.remove(configsList.getSelectedIndex());
					updateAndSave();
				}});

		if (configs.size() > 0) {
			configsList.setSelectedIndex(0);
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) { app.runClose(); }
		});

		setJMenuBar(new ConfigsListMenuBar(this));

		pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(600, 400);
	}

	void open() {
		if (configsList.getSelectedIndex() != -1) {
			final StoreConfig sc = (StoreConfig) configsListM.getElementAt(
								configsList.getSelectedIndex());
			pm.runBlockingTask("Opening store", new BlockingTask() {
				public boolean run() {
					return app.openStore(sc) != null;
				}

				public void onSuccess() {}
				public void onFailure() {}
			});
			
		}
	}

	void add() {
		StorageSystem ss = (StorageSystem) JOptionPane.showInputDialog(this,
				"What kind of store do you want to add?",
				"Choose store type", JOptionPane.QUESTION_MESSAGE, null,
				Utils.getAvailableStorageSystems().toArray(),
				Utils.getAvailableStorageSystems().get(0));
		if (ss != null) {
			final StoreConfig sc = ConfigDialog.newConfig(ss, this);
			if (sc != null) {
				configs.add(sc);
				updateAndSave();
				pm.runBlockingTask("Opening store", new BlockingTask() {
					public boolean run() {
						return app.openStore(sc) != null;
					}

					public void onSuccess() {}
					public void onFailure() {}
				});
			}
		}
	}

	void edit() {
		int i = configsList.getSelectedIndex();
		if (i != -1) {
			configs.set(i, ConfigDialog.editConfig(configs.get(i), this));
			updateAndSave();
		}
	}

	void updateAndSave() {
		configsListM.update();
		try {
			SavedStoreConfigs.setSavedStoreConfigs(configs);
		} catch (Exception e) {
			pm.handleException(e, null);
		}
	}

	static class ConfigsListModel extends AbstractListModel {
		final ConfigsListFrame slf;

		ConfigsListModel(ConfigsListFrame slf) { this.slf = slf; }

		public int getSize() { return slf.configs.size(); }

		public Object getElementAt(int index) { return slf.configs.get(index); }

		void update() {
			fireContentsChanged(this, 0, getSize());
		}
	}
}
