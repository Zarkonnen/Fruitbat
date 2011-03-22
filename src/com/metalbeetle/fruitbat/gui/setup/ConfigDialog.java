package com.metalbeetle.fruitbat.gui.setup;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.gui.WindowExpirationWrapper;
import com.metalbeetle.fruitbat.gui.setup.ConfigPanel.ConfigChangedListener;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class ConfigDialog extends JDialog implements ConfigChangedListener {
	final ConfigPanel cp;
	final JPanel buttonP;
		final JButton okB;
		final JButton cancelB;
	StoreConfig config = null;

	public static StoreConfig newConfig(StorageSystem sys, JFrame parent, Fruitbat app) {
		ConfigDialog cf = new ConfigDialog(sys, parent, app);
		cf.setLocationRelativeTo(parent);
		cf.setVisible(true);
		return cf.config;
	}

	public static StoreConfig editConfig(StoreConfig conf, JFrame parent, Fruitbat app) {
		ConfigDialog cf = new ConfigDialog(conf, parent, app);
		cf.setLocationRelativeTo(parent);
		cf.setVisible(true);
		return cf.config == null ? conf : cf.config;
	}

	ConfigDialog(StoreConfig conf, JFrame parent, Fruitbat app) {
		this(conf.system, parent, app);
		cp.setConfig(conf);
	}

	ConfigDialog(StorageSystem sys, JFrame parent, Fruitbat app) {
		super(parent, "Configure " + sys);
		setModal(true);
		setLayout(new BorderLayout());
		add(cp = new ConfigPanel(sys), BorderLayout.CENTER);
			cp.setConfigChangedListener(this);
			cp.addUndoableEditListener(new WindowExpirationWrapper(parent, app.undoManager));
		add(buttonP = new JPanel(new FlowLayout()), BorderLayout.SOUTH);
			buttonP.add(cancelB = new JButton("Cancel"));
				cancelB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					dispose();
				}});
			buttonP.add(okB = new JButton("OK"));
				okB.setEnabled(false);
				getRootPane().setDefaultButton(okB);
				okB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					if (cp.allValid()) {
						config = cp.getConfig();
						dispose();
					}
				}});
		getRootPane().registerKeyboardAction(new ActionListener() { public void actionPerformed(ActionEvent e) {
				dispose(); } },
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		//setSize(700, 600);
		pack();
		Dimension size = getSize();
		size.width  += cp.getExtraSize().width;
		size.height += cp.getExtraSize().height;
		setSize(size);
	}

	public void configChanged(boolean allValid) {
		okB.setEnabled(allValid);
	}
}
