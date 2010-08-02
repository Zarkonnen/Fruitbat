package com.metalbeetle.fruitbat.gui.setup;

import com.metalbeetle.fruitbat.storage.ConfigField;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import com.metalbeetle.fruitbat.storage.Utils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.*;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class ConfigPanel extends JPanel implements FieldJComponent.ValueListener {
	final List<ConfigField> fields;
	final List<JLabel> nameLs;
	final List<FieldJComponent> fieldJCs;
	final List<JLabel> errLs;
	final StorageSystem system;

	public void setConfigChangedListener(ConfigChangedListener ccl) { this.ccl = ccl; }
	ConfigChangedListener ccl;

	public ConfigPanel(StorageSystem system) {
		this.system = system;
		fields = system.getConfigFields();
		setLayout(new GridBagLayout());
		ArrayList<JLabel> nls = new ArrayList<JLabel>(fields.size());
		ArrayList<FieldJComponent> fcs = new ArrayList<FieldJComponent>(fields.size());
		ArrayList<JLabel> errs = new ArrayList<JLabel>(fields.size());
		int y = 0;
		for (ConfigField f : fields) {
			GridBagConstraints cs;

			JLabel l = new JLabel(f.getName() + ":");
			cs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ y,
					 /*gridwidth*/ 1,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ WEST,
					      /*fill*/ NONE,
					    /*insets*/ new Insets(5, 10, 0, 0),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
			add(l, cs);
			nls.add(l);

			FieldJComponent fjc = f.getFieldJComponent();
			((JComponent) fjc).setBorder(new BevelBorder(BevelBorder.LOWERED));
			fjc.setValueListener(this);
			cs = new GridBagConstraints(
					     /*gridx*/ 1,
					     /*gridy*/ y,
					 /*gridwidth*/ 1,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ WEST,
					      /*fill*/ HORIZONTAL,
					    /*insets*/ new Insets(5, 10, 0, 10),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
			add((JComponent) fjc, cs);
			fcs.add(fjc);

			String err = f.validate(fjc.getValue());
			JLabel errL = new JLabel(err == null ? "" : err);
			errL.setForeground(Color.RED);
			cs = new GridBagConstraints(
					     /*gridx*/ 2,
					     /*gridy*/ y,
					 /*gridwidth*/ 1,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ WEST,
					      /*fill*/ NONE,
					    /*insets*/ new Insets(5, 0, 0, 10),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
			add(errL, cs);
			errs.add(errL);

			y++;
		}

		GridBagConstraints cs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ y,
					 /*gridwidth*/ 3,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 100,
					    /*anchor*/ CENTER,
					      /*fill*/ BOTH,
					    /*insets*/ new Insets(0, 0, 0, 0),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);

		add(new JPanel(), cs);

		nameLs = immute(nls);
		fieldJCs = immute(fcs);
		errLs = immute(errs);

		for (FieldJComponent fjc : fieldJCs) { valueChanged(fjc); }
	}

	public void setConfig(StoreConfig sc) {
		for (int i = 0; i < sc.configFieldValues.size(); i++) {
			fieldJCs.get(i).setValue(sc.configFieldValues.get(i));
		}
	}

	public StoreConfig getConfig() {
		ArrayList<Object> vals = new ArrayList<Object>();
		for (FieldJComponent c : fieldJCs) {
			vals.add(c.getValue());
		}
		return new StoreConfig(system, vals);
	}

	public void valueChanged(FieldJComponent fjc) {
		int index = fieldJCs.indexOf(fjc);
		String err = fields.get(index).validate(fjc.getValue());
		errLs.get(index).setText(err == null ? "√" : err);
		errLs.get(index).setForeground(err == null ? Color.GREEN : Color.RED);
		revalidate();
		if (ccl != null) { ccl.configChanged(allValid()); }
	}

	public boolean allValid() {
		/*boolean allValid = true;
		for (FieldJComponent f : fieldJCs) {
			allValid = allValid && f.getField().validate(f.getValue()) == null;
		}
		return allValid;*/
		try {
			Utils.checkConfigValues(getConfig());
			return true;
		} catch (StoreConfigInvalidException e) {
			return false;
		}
	}

	public static interface ConfigChangedListener {
		public void configChanged(boolean allValid);
	}
}
