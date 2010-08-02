package com.metalbeetle.fruitbat.gui;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

/** JTextPane with fixed tab focusing behaviour and shortcut behaviour. */
class FixedTextPane extends JTextPane {
	FixedTextPane() {
		Set<KeyStroke> strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("pressed TAB")));
		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes);
		strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("shift pressed TAB")));
		setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes);
		setBorder(new JTextField().getBorder());
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				getParent().dispatchEvent(e);
			}
		});
	}
}
