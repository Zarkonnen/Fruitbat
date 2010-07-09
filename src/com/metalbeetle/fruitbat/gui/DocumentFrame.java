package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.Document;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

class DocumentFrame extends JFrame {
	final Document d;
	final OpenDocManager manager;

	final Box searchBoxH;
		final Box searchBoxV;
			final JTextPane tagsF;
		final JPanel buttonP;
			final JButton addPageB;
	final JSplitPane split;
		final JScrollPane suggestedTagsListSP;
			final JList suggestedTagsList;
		final PagesViewer viewer;

	public DocumentFrame(final Document d, final OpenDocManager manager) throws HeadlessException {
		super("Fruitbat Document");
		this.d = d;
		this.manager = manager;

		Container c = getContentPane();
		c.setLayout(new BorderLayout(0, 5));
		// This layouting is horrible and should be replaced by a grid bag.
		c.add(searchBoxH = Box.createHorizontalBox(), BorderLayout.NORTH);
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(searchBoxV = Box.createVerticalBox());
				searchBoxV.add(Box.createVerticalStrut(5));
				searchBoxV.add(tagsF = new JTextPane());
					// Fix tab focusing behaviour.
					Set<KeyStroke> strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("pressed TAB")));
					tagsF.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes);
					strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("shift pressed TAB")));
					tagsF.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes);
					tagsF.setBorder(new JTextField().getBorder());
					tagsF.setDocument(new TagColorizingDocument(tagsF));
				searchBoxV.add(Box.createVerticalStrut(5));
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(buttonP = new JPanel(new FlowLayout()));
				buttonP.add(addPageB = new JButton("Add Page"));
					addPageB.setFocusable(false);
					addPageB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						tagsF.requestFocusInWindow();
					}});
		c.add(split = new JSplitPane(), BorderLayout.CENTER);
			split.setBorder(new EmptyBorder(0, 5, 5, 5));
				split.setLeftComponent(suggestedTagsListSP = new JScrollPane());
					suggestedTagsListSP.setViewportView(suggestedTagsList = new JList(
							new String[] { "[list of suggested/available tags" }));
				split.setRightComponent(viewer = new PagesViewer(this));

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				save();
				manager.close(d);
			}

			@Override
			public void windowIconified(WindowEvent e) {
				save();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				save();
			}
		});

		updateTags();
		pack();
		setSize(800, 600);
	}

	void save() {
		// TODO
	}

	void updateTags() {
		if (d.keys().size() == 0) { tagsF.setText(""); }
		StringBuilder sb = new StringBuilder();
		for (String k : d.keys()) {
			sb.append(k);
			String v = d.get(k);
			if (v.length() > 0) {
				sb.append(":");
				sb.append(v);
			}
			sb.append(" ");
		}
		tagsF.setText(sb.substring(0, sb.length() - 1));
	}
}
