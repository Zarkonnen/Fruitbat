package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.gui.setup.FieldJComponent;
import com.metalbeetle.fruitbat.gui.setup.FileFieldComponent;
import java.io.File;
import javax.swing.filechooser.FileFilter;

public interface ConfigField<T> {
	/** @return The name of the field. */
	public String getName();
	/** @return null if valid, or an error message if not. */
	public String validate(T input);
	/** @return The class of T. */
	public Class getExpectedValueClass();
	/** Converts a T value to a String. */
	public String toString(T value) throws StoreConfigInvalidException;
	/** Converts a String to a T value. */
	public T toValue(String s) throws StoreConfigInvalidException;
	/** @return FieldJComponent for displaying this field. */
	public FieldJComponent<T> getFieldJComponent();

	public abstract class StringField implements ConfigField<String>{
		final String name;
		public StringField(String name) { this.name = name; }
		public String getName() { return name; }
		public Class getExpectedValueClass() { return String.class; }
		public String toString(String value) { return value; }
		public String toValue(String s) { return s; }
	}

	public abstract class FileField implements ConfigField<File> {
		final String name;
		final String fileTypeDescription;
		public FileField(String name, String fileTypeDescription) { this.name = name; this.fileTypeDescription = fileTypeDescription; }
		public String getName() { return name; }
		public Class getExpectedValueClass() { return File.class; }
		public FileFilter getFileFilter() {
			return new FileFilter() {
				@Override
				public boolean accept(File f) { return validate(f) == null; }
				@Override
				public String getDescription() { return fileTypeDescription; }
			};
		}
		public abstract boolean lookingForFolders();
		public String toString(File value) { return value.getAbsolutePath(); }
		public File toValue(String s) { return new File(s); }
		public FieldJComponent<File> getFieldJComponent() {
			FileFieldComponent ffc = new FileFieldComponent();
			ffc.setField(this);
			return ffc;
		}
	}
}
