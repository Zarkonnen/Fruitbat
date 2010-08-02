package com.metalbeetle.fruitbat.gui.setup;

import com.metalbeetle.fruitbat.storage.ConfigField;

/** Interface for a JComponent that can be used to input a field value. */
public interface FieldJComponent<T> {
	public T getValue();
	public void setValue(T value);
	public void setField(ConfigField<T> field);
	public ConfigField<T> getField();
	public void setValueListener(ValueListener l);
	
	public static interface ValueListener {
		public void valueChanged(FieldJComponent fjc);
	}
}
