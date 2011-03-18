package com.metalbeetle.fruitbat.gui.setup;

import com.metalbeetle.fruitbat.gui.setup.FieldJComponent.ValueListener;
import com.metalbeetle.fruitbat.storage.ConfigField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class StringFieldComponent extends JPanel implements FieldJComponent<String>, DocumentListener {
	final JTextField textField;
	ConfigField<String> field = null;
	ValueListener vl = null;

	public StringFieldComponent() {
		//setLayout(new FlowLayout(FlowLayout.LEFT));
		setLayout(new GridBagLayout());
		textField = new JTextField(20);
		textField.getDocument().addDocumentListener(this);
		textField.setEnabled(false);
		GridBagConstraints cs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ 0,
					 /*gridwidth*/ 1,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ GridBagConstraints.NORTHWEST,
					      /*fill*/ GridBagConstraints.HORIZONTAL,
					    /*insets*/ new Insets(0, 0, 0, 0),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
		add(textField, cs);
	}

	public String getValue() {
		return textField.getText();
	}

	public void setValue(String value) {
		ValueListener tmpVL = vl;
		textField.setText(value);
		vl = tmpVL;
	}

	public void setField(ConfigField<String> field) {
		this.field = field;
		textField.setEnabled(field != null);
	}

	public ConfigField<String> getField() {
		return field;
	}

	public void setValueListener(ValueListener vl) {
		this.vl = vl;
	}

	public void insertUpdate(DocumentEvent e) {
		if (vl != null) { vl.valueChanged(this); }
	}

	public void removeUpdate(DocumentEvent e) {
		if (vl != null) { vl.valueChanged(this); }
	}

	public void changedUpdate(DocumentEvent e) {
		if (vl != null) { vl.valueChanged(this); }
	}

	public Dimension getExtraSize() { return new Dimension(0, 0); }
}
