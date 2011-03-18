package com.metalbeetle.fruitbat.multiplexstorage;

import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import com.metalbeetle.fruitbat.gui.setup.FieldJComponent;
import com.metalbeetle.fruitbat.storage.ConfigField;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import com.metalbeetle.fruitbat.storage.StoreConfigInvalidException;
import com.metalbeetle.fruitbat.storage.Utils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class MultiplexedStoresField implements ConfigField<List<StoreConfig>> {

	final String name;

	public String getName() {
		return name;
	}

	public Class getExpectedValueClass() {
		return List.class;
	}

	public MultiplexedStoresField(String name) {
		this.name = name;
	}

	public String validate(List<StoreConfig> input) {
		if (input.size() < 2) { return "Configure at least two stores"; }
		for (StoreConfig sc : input) {
			try {
				Utils.checkConfigValues(sc);
			} catch (StoreConfigInvalidException ex) {
				return sc.system + " not configured";
			}
		}
		return null;
	}

	public String toString(List<StoreConfig> value) throws StoreConfigInvalidException {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ATRWriter w = new ATRWriter(out);
			w.startRecord();
			for (StoreConfig sc : value) {
				w.write(sc.toStringRepresentation());
			}
			w.endRecord();
			w.close();
			return new String(out.toByteArray());
		} catch (Exception e) {
			throw new StoreConfigInvalidException("Could not save multiplexed store " + "configuration.");
		}
	}

	public List<StoreConfig> toValue(String s) throws StoreConfigInvalidException {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes());
			ATRReader r = new ATRReader(in);
			ArrayList<StoreConfig> vals = new ArrayList<StoreConfig>();
			for (String rec : r.readRecord()) {
				vals.add(new StoreConfig(rec));
			}
			return immute(vals);
		} catch (Exception e) {
			throw new StoreConfigInvalidException("Could not read multiplexed store configuration.");
		}
	}

	public FieldJComponent<List<StoreConfig>> getFieldJComponent() {
		MultiplexedStoresFieldComponent c = new MultiplexedStoresFieldComponent();
		c.setField(this);
		return c;
	}

	public List<StoreConfig> clean(List<StoreConfig> t) { return t; }
}
