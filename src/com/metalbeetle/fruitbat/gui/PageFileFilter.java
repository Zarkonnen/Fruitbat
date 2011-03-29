package com.metalbeetle.fruitbat.gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class PageFileFilter extends FileFilter {
	public boolean accept(File f) {
		if (f.isDirectory()) { return true; }
		for (String ext : DocumentFrame.ACCEPTED_EXTENSIONS) {
			if (f.getName().toLowerCase().endsWith(ext.toLowerCase())) { return true; }
		}
		return false;
	}

	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		for (String ext : DocumentFrame.ACCEPTED_EXTENSIONS) {
			sb.append(ext);
			sb.append(" ");
		}
		sb.append("files");
		return sb.toString();
	}
}
