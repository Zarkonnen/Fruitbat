package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.util.Pair;
import java.awt.Component;
import java.awt.Event;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.JTextComponent;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class EditMenu extends JMenu {
	final JMenuItem undoItem;
	final JMenuItem redoItem;
	final JMenuItem cutItem;
	final JMenuItem copyItem;
	final JMenuItem pasteItem;
	final JMenuItem selectAllItem;
	final Fruitbat app;

	JTextComponent editingComponent = null;

	public void update() {
		if (app.undoManager.canUndo()) {
			undoItem.setEnabled(true);
			undoItem.setText(app.undoManager.getUndoPresentationName());
		} else {
			undoItem.setEnabled(false);
			undoItem.setText("Undo");
		}
		if (app.undoManager.canRedo()) {
			redoItem.setEnabled(true);
			redoItem.setText(app.undoManager.getRedoPresentationName());
		} else {
			redoItem.setEnabled(false);
			redoItem.setText("Redo");
		}
	}

	public EditMenu(final Fruitbat app) {
		super("Edit");
		this.app = app;
		app.undoManager.register(this);

		addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent me) {
				update();
			}
			public void menuDeselected(MenuEvent me) {}
			public void menuCanceled(MenuEvent me) {}
		});

		add(undoItem = new JMenuItem("Undo"));
		undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		undoItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				app.undoManager.undo();
			}
		});
		add(redoItem = new JMenuItem("Redo"));
		redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | Event.SHIFT_MASK));
		redoItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				app.undoManager.redo();
			}
		});
		addSeparator();
		add(cutItem = new JMenuItem("Cut"));
		cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		add(copyItem = new JMenuItem("Copy"));
		copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		add(pasteItem = new JMenuItem("Paste"));
		pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		add(selectAllItem = new JMenuItem("Select All"));
		selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
						Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		// qqDPS Memory leak?
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent pce) {
				focusChangedTo((Component) pce.getNewValue());
			}
		});

		update();
	}

	void focusChangedTo(Component c) {
		if (c instanceof JTextComponent) {
			editingComponent = (JTextComponent) c;
		} else {
			c = null;
		}

		for (Pair<String, JMenuItem> jmi : l(
				p("cut-to-clipboard", cutItem),
				p("copy-to-clipboard", copyItem),
				p("paste-from-clipboard", pasteItem),
				p("select-all", selectAllItem)))
		{
			if (editingComponent == null) {
				jmi.b.setEnabled(false);
			} else {
				for (ActionListener al : jmi.b.getActionListeners()) {
					jmi.b.removeActionListener(al);
				}
				for (Action a : editingComponent.getActions()) {
					if (a.getValue(Action.NAME).equals(jmi.a)) {
						jmi.b.setEnabled(true);
						jmi.b.addActionListener(a);
					}
				}
			}
		}
	}
}
