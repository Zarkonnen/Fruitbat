package com.metalbeetle.fruitbat.gui;

import java.util.WeakHashMap;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/** UndoManager that notifies edit menus of changes to the undo stack. */
public class EnhancedUndoManager extends UndoManager {
	WeakHashMap<EditMenu, Object> editMenus = new WeakHashMap<EditMenu, Object>();

	public void register(EditMenu em) {
		editMenus.put(em, new Object());
	}

	@Override
	public void undo() {
		super.undo();
		update();
	}

	@Override
	public void redo() {
		super.redo();
		update();
	}

	@Override
	public void undoableEditHappened(UndoableEditEvent uee) {
		super.undoableEditHappened(uee);
		update();
	}

	@Override
	public boolean addEdit(UndoableEdit uee) {
		if (super.addEdit(uee)) {
			update();
			return true;
		}
		return false;
	}

	public void update() {
		for (EditMenu em : editMenus.keySet()) { em.update(); }
	}
}
