package com.metalbeetle.fruitbat.gui.setup;

import java.awt.Dimension;
import com.metalbeetle.fruitbat.storage.ConfigField;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StorageSystem;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import com.metalbeetle.fruitbat.storage.Utils;
import java.awt.Color;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.*;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class ConfigPanel extends JPanel implements FieldJComponent.ValueListener {
	final List<ConfigField> fields;
	final List<JLabel> nameLs;
	final List<FieldJComponent> fieldJCs;
	final List<JLabel> errLs;
	final JLabel nameL;
	final JTextField nameF;
	final StorageSystem system;
	final Dimension extraSize;

	public void setConfigChangedListener(ConfigChangedListener ccl) { this.ccl = ccl; }
	ConfigChangedListener ccl;

	public ConfigPanel(StorageSystem system) {
		this.system = system;
		fields = system.getConfigFields();
		setLayout(new GridBagLayout());
		ArrayList<JLabel> nls = new ArrayList<JLabel>(fields.size());
		ArrayList<FieldJComponent> fcs = new ArrayList<FieldJComponent>(fields.size());
		ArrayList<JLabel> errs = new ArrayList<JLabel>(fields.size());
		GridBagConstraints cs;
		nameL = new JLabel("Name");
		cs = new GridBagConstraints(
					 /*gridx*/ 0,
					 /*gridy*/ 0,
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
		add(nameL, cs);

		nameF = new JTextField(20);
		cs = new GridBagConstraints(
					 /*gridx*/ 1,
					 /*gridy*/ 0,
				 /*gridwidth*/ 1,
				/*gridheight*/ 1,
				   /*weightx*/ 1,
				   /*weighty*/ 1,
					/*anchor*/ NORTHWEST,
					  /*fill*/ HORIZONTAL,
					/*insets*/ new Insets(5, 10, 0, 10),
					 /*ipadx*/ 0,
					 /*ipady*/ 0
				);
		add(nameF, cs);

		cs = new GridBagConstraints(
					 /*gridx*/ 0,
					 /*gridy*/ 1,
				 /*gridwidth*/ 2,
				/*gridheight*/ 1,
				   /*weightx*/ 0,
				   /*weighty*/ 0,
					/*anchor*/ WEST,
					  /*fill*/ HORIZONTAL,
					/*insets*/ new Insets(0, 0, 20, 0),
					 /*ipadx*/ 0,
					 /*ipady*/ 0
				);
		add(new JSeparator(), cs);

		int extraWidth = 0, extraHeight = 0;
		int y = 0;
		for (ConfigField f : fields) {
			// Field label
			JLabel l = new JLabel(f.getName() + ":");
			cs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ 2 + y * 3,
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

			// Get the field component
			FieldJComponent fjc = f.getFieldJComponent();
			fjc.setValueListener(this);
			// Figure out how much extra size we want to give to the panel.
			extraWidth  += fjc.getExtraSize().width;
			extraHeight += fjc.getExtraSize().height;

			// Error label
			String err = f.validate(fjc.getValue());
			JLabel errL = new JLabel(err == null ? "" : err);
			errL.setFont(errL.getFont().deriveFont(errL.getFont().getSize() * 0.75f));
			errL.setForeground(Color.RED);
			cs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ 2 + y * 3 + 1,
					 /*gridwidth*/ 1,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ WEST,
					      /*fill*/ NONE,
					    /*insets*/ new Insets(0, 10, 0, 0),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
			add(errL, cs);
			errs.add(errL);

			// Field component
			cs = new GridBagConstraints(
					     /*gridx*/ 1,
					     /*gridy*/ 2 + y * 3,
					 /*gridwidth*/ 1,
					/*gridheight*/ 2,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ NORTHWEST,
					      /*fill*/ HORIZONTAL,
					    /*insets*/ new Insets(5, 10, 0, 10),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
			add((JComponent) fjc, cs);
			fcs.add(fjc);

			// Separator
			cs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ 2 + y * 3 + 2,
					 /*gridwidth*/ 2,
					/*gridheight*/ 1,
					   /*weightx*/ 0,
					   /*weighty*/ 0,
					    /*anchor*/ WEST,
					      /*fill*/ HORIZONTAL,
					    /*insets*/ new Insets(0, 0, 0, 0),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
			add(new JSeparator(), cs);

			y++;
		}

		cs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ 2 + y * 3,
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
		extraSize = new Dimension(extraWidth, extraHeight);
	}

	public Dimension getExtraSize() { return extraSize; }

	public void setConfig(StoreConfig sc) {
		nameF.setText(sc.name);
		for (int i = 0; i < sc.configFieldValues.size(); i++) {
			fieldJCs.get(i).setValue(sc.configFieldValues.get(i));
			valueChanged(fieldJCs.get(i));
		}
	}

	public StoreConfig getConfig() {
		ArrayList<Object> vals = new ArrayList<Object>();
		for (FieldJComponent c : fieldJCs) {
			vals.add(c.getValue());
		}
		return new StoreConfig(nameF.getText(), system, vals);
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
		try {
			Utils.checkConfigValues(getConfig());
			return !nameF.getText().isEmpty();
		} catch (StoreConfigInvalidException e) {
			return false;
		}
	}

	public static interface ConfigChangedListener {
		public void configChanged(boolean allValid);
	}
}
