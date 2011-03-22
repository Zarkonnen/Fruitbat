package com.metalbeetle.fruitbat.gui;

import java.awt.Window;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

/** Wraps UndoableEdits so that they expire when the relevant window is closed. */
public class WindowExpirationWrapper implements UndoableEditListener {
	final Window window;
	final EnhancedUndoManager eum;

	public WindowExpirationWrapper(Window window, EnhancedUndoManager eum) {
		this.window = window;
		this.eum = eum;
	}

	public void undoableEditHappened(UndoableEditEvent uee) {
		eum.addEdit(new InWindowUndoWrapper(uee.getEdit(), window, eum));
	}

	@Override
	public boolean equals(Object o2) {
		if (!(o2 instanceof WindowExpirationWrapper)) { return false; }
		return ((WindowExpirationWrapper) o2).window == window;
	}

	@Override
	public int hashCode() {
		return window.hashCode();
	}
}
