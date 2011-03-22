package com.metalbeetle.fruitbat.gui;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

public class InWindowUndoWrapper implements UndoableEdit, WindowListener {

	final UndoableEdit innerUndo;
	final Window window;
	final EnhancedUndoManager eum;
	boolean frameClosed = false;

	public InWindowUndoWrapper(UndoableEdit innerUndo, Window window, EnhancedUndoManager eum) {
		this.innerUndo = innerUndo;
		this.window = window;
		this.eum = eum;
		window.addWindowListener(this);
	}

	public void undo() throws CannotUndoException {
		if (frameClosed) { throw new CannotUndoException(); }
		innerUndo.undo();
	}

	public boolean replaceEdit(UndoableEdit ue) {
		return !frameClosed && innerUndo.replaceEdit(ue);
	}

	public void redo() throws CannotRedoException {
		if (frameClosed) { throw new CannotRedoException(); }
		innerUndo.redo();
	}

	public boolean isSignificant() {
		return innerUndo.isSignificant();
	}

	public String getUndoPresentationName() {
		return innerUndo.getUndoPresentationName();
	}

	public String getRedoPresentationName() {
		return innerUndo.getRedoPresentationName();
	}

	public String getPresentationName() {
		return innerUndo.getPresentationName();
	}

	public void die() {
		innerUndo.die();
	}

	public boolean canUndo() {
		return !frameClosed && innerUndo.canUndo();
	}

	public boolean canRedo() {
		return !frameClosed && innerUndo.canRedo();
	}

	public boolean addEdit(UndoableEdit ue) {
		return !frameClosed && innerUndo.addEdit(ue);
	}

	public void windowOpened(WindowEvent we) {}
	public void windowClosing(WindowEvent we) {}

	public void windowClosed(WindowEvent we) {
		frameClosed = true;
		die();
		window.removeWindowListener(this);
		eum.update();
	}

	public void windowIconified(WindowEvent we) {}
	public void windowDeiconified(WindowEvent we) {}
	public void windowActivated(WindowEvent we) {}
	public void windowDeactivated(WindowEvent we) {}
}
