package com.metalbeetle.fruitbat.gui.setup;

import com.metalbeetle.fruitbat.gui.setup.FieldJComponent.ValueListener;
import com.metalbeetle.fruitbat.storage.ConfigField;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FileFieldComponent extends JPanel implements FieldJComponent<File>, ActionListener {
	static final String NO_FILE = "no file selected";

	File value = null;
	ConfigField.FileField field = null;
	ValueListener l = null;

	final JLabel pathL;
	final JButton chooseB;

	public FileFieldComponent() {
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(pathL = new JLabel());
		add(chooseB = new JButton("Choose..."));
			chooseB.addActionListener(this);
		update();
	}

	public File getValue() { return value; }

	public void setValue(File value) {
		this.value = value;
		update();
	}

	public void setField(ConfigField<File> field) {
		this.field = (ConfigField.FileField) field;
		update();
	}

	public ConfigField<File> getField() {
		return field;
	}

	public Class getConfigFieldClass() {
		return ConfigField.FileField.class;
	}

	void update() {
		pathL.setText(value == null ? NO_FILE : value.getPath());
		pathL.setFont(pathL.getFont().deriveFont(value == null ? Font.ITALIC : Font.PLAIN));
		chooseB.setEnabled(field != null);
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser jfc = new JFileChooser();
		if (value != null) {
			jfc.setCurrentDirectory(value.getAbsoluteFile().getParentFile());
		}
		jfc.setDialogTitle("Choose " + field.getName());
		jfc.setFileFilter(field.getFileFilter());
		if (field.lookingForFolders()) { jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); }
		if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			setValue(jfc.getSelectedFile());
			if (l != null) { l.valueChanged(this); }
		}
	}

	public void setValueListener(ValueListener l) { this.l = l; }
}
