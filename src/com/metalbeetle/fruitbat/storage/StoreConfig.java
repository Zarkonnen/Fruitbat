package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.ProgressMonitor;
import com.metalbeetle.fruitbat.atrio.ATRReader;
import com.metalbeetle.fruitbat.atrio.ATRWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

public final class StoreConfig {
	public final String name;
	public final StorageSystem system;
	public final List<Object> configFieldValues;

	public StoreConfig(String name, StorageSystem system, List<Object> configFieldValues) {
		this.name = name;
		this.system = system;
		this.configFieldValues = immute(configFieldValues);
	}

	public StoreConfig(StorageSystem system, List<Object> configFieldValues) {
		this.system = system;
		this.configFieldValues = immute(configFieldValues);
		this.name = getSummary();
	}

	public StoreConfig(String stringRepresentation) throws StoreConfigInvalidException {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(stringRepresentation.getBytes());
			ATRReader r = new ATRReader(in);
			List<String> rec = r.readRecord();
			name = rec.get(0);
			system = (StorageSystem) Class.forName(rec.get(1)).newInstance();
			ArrayList<Object> vals = new ArrayList<Object>();
			for (int i = 2; i < rec.size(); i++) {
				vals.add(system.getConfigFields().get(i - 2).toValue(rec.get(i)));
			}
			configFieldValues = immute(vals);
		} catch (Exception e) {
			throw new StoreConfigInvalidException("Could not read store configuration.");
		}
		Utils.checkConfigValues(this);
	}

	public String toStringRepresentation() throws StoreConfigInvalidException {
		Utils.checkConfigValues(this);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ATRWriter w = new ATRWriter(out);
			w.startRecord();
			w.write(name);
			w.write(system.getClass().getName());
			for (int i = 0; i < configFieldValues.size(); i++) {
				w.write(system.getConfigFields().get(i).toString(configFieldValues.get(i)));
			}
			w.endRecord();
			w.close();
			return new String(out.toByteArray());
		} catch (IOException e) {
			throw new StoreConfigInvalidException("Could not save store configuration.");
		}
	}

	public EnhancedStore init(ProgressMonitor pm) throws FatalStorageException, StoreConfigInvalidException {
		return new EnhancedStore(system.init(configFieldValues, pm));
	}

	@Override
	public String toString() {
		return name;
	}

	public String getSummary() {
		StringBuilder sb = new StringBuilder(system.toString());
		sb.append(" (");
		for (int i = 0; i < system.getConfigFields().size(); i++) {
			sb.append(system.getConfigFields().get(i).getName());
			sb.append(": ");
			sb.append(configFieldValues.get(i));
			sb.append(", ");
		}
		if (system.getConfigFields().size() > 0) {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof StoreConfig)) { return false; }
		StoreConfig c2 = (StoreConfig) o2;
		if (!nullAwareEquals(system, c2.system)) { return false; }
		if (c2.configFieldValues.size() != configFieldValues.size()) { return false; }
		for (int i = 0; i < configFieldValues.size(); i++) {
			if (!nullAwareEquals(configFieldValues.get(i), c2.configFieldValues.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 17 * hash + (system == null ? 0 : system.hashCode());
		if (configFieldValues != null) {
			for (Object v : configFieldValues) {
				hash = 17 * hash + (v == null ? 0 : v.hashCode());
			}
		}
		return hash;
	}
}
