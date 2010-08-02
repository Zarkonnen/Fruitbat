package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.gui.setup.ConfigPanel;
import com.metalbeetle.fruitbat.gui.setup.FieldJComponent;
import com.metalbeetle.fruitbat.gui.setup.FieldJComponent.ValueListener;
import com.metalbeetle.fruitbat.storage.ConfigField;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import com.metalbeetle.fruitbat.storage.Utils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.BevelBorder;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class MultiplexedStoresFieldComponent extends JPanel implements
		FieldJComponent<List<StoreConfig>>, ConfigPanel.ConfigChangedListener
{
	ValueListener vl;
	MultiplexedStoresField f;
	
	final JTabbedPane tabs;
	final JPanel buttonP;
		final JButton addB;
		final JButton removeB;

	public MultiplexedStoresFieldComponent() {
		setLayout(new BorderLayout());
			add(tabs = new JTabbedPane(JTabbedPane.TOP), BorderLayout.CENTER);
			add(buttonP = new JPanel(new FlowLayout(FlowLayout.RIGHT)), BorderLayout.SOUTH);
				buttonP.add(addB = new JButton("Add Store"));
					addB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						addStore();
					}});
				buttonP.add(removeB = new JButton("Remove Store"));
					removeB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						removeStore();
					}});
	}

	String getTabName(int index, StorageSystem sys) {
		return (index == 0 ? "Master " : "Backup " + index) + " (" + sys + ")";
	}

	void addStore() {
		StorageSystem ss = (StorageSystem) JOptionPane.showInputDialog(null,
				"What kind of store do you want to add?",
				"Choose store type", JOptionPane.QUESTION_MESSAGE, null,
				Utils.getAvailableStorageSystems().toArray(),
				Utils.getAvailableStorageSystems().get(0));
		if (ss != null) {
			ConfigPanel cp = new ConfigPanel(ss);
			cp.setConfigChangedListener(this);
			tabs.addTab(getTabName(tabs.getTabCount(), ss), cp);
			tabs.setSelectedComponent(cp);
		}
		updateButtons();
		if (vl != null) { vl.valueChanged(this); }
	}

	void removeStore() {
		if (tabs.getSelectedIndex() != -1) {
			tabs.removeTabAt(tabs.getSelectedIndex());
		}
		updateButtons();
		if (vl != null) { vl.valueChanged(this); }
	}

	public List<StoreConfig> getValue() {
		ArrayList<StoreConfig> l = new ArrayList<StoreConfig>();
		for (int i = 0; i < tabs.getTabCount(); i++) {
			l.add(((ConfigPanel) tabs.getComponent(i)).getConfig());
		}
		return l;
	}

	public void setValue(List<StoreConfig> value) {
		tabs.removeAll();
		for (int i = 0; i < value.size(); i++) {
			StoreConfig sc = value.get(i);
			ConfigPanel cp = new ConfigPanel(sc.system);
			cp.setConfig(sc);
			cp.setConfigChangedListener(this);
			tabs.addTab(getTabName(i, sc.system), cp);
		}
		updateButtons();
	}

	void updateButtons() {
		removeB.setEnabled(tabs.getSelectedIndex() != -1);
	}

	public void setField(ConfigField<List<StoreConfig>> f) {
		this.f = (MultiplexedStoresField) f;
	}

	public ConfigField<List<StoreConfig>> getField() {
		return f;
	}

	public void setValueListener(ValueListener vl) { this.vl = vl; }

	public void configChanged(boolean allValid) {
		if (vl != null) { vl.valueChanged(this); }
	}
}
