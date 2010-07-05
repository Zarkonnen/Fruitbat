package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.util.Pair;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class MainFrame extends JFrame {
	final Fruitbat app;

	List<TagSuggestor> suggestors;
	List<String> suggestions = new ArrayList<String>();

	Pair<List<Document>, List<String>> currentSearchResult;
	HashMap<String, String> lastSearchKV = new HashMap<String, String>();
	List<String> lastSearchKeys = new ArrayList<String>();
	String lastSearch = "";

	final Box searchBoxH;
		final Box searchBoxV;
			final JTextPane searchF;
	final JPanel buttonP;
		final JButton newDocumentB;
	final JSplitPane split;
		final JPanel docsP;
			final JLabel docsL;
			final JScrollPane docsListSP;
				final JList docsList;
					final DocsListModel docsListM;
		final JPanel tagsP;
			final JLabel tagsL;
			final JScrollPane tagsListSP;
				final JList tagsList;
					final TagsListModel tagsListM;

	public MainFrame(Fruitbat application) throws HeadlessException {
		super("Fruitbat");
		app = application;
		suggestors = l((TagSuggestor) new DateSuggestor());
		search("");

		Container c = getContentPane();
		c.setLayout(new BorderLayout(0, 5));
		// This layouting is horrible and should be replaced by a grid bag.
		c.add(searchBoxH = Box.createHorizontalBox(), BorderLayout.NORTH);
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(searchBoxV = Box.createVerticalBox());
				searchBoxV.add(Box.createVerticalStrut(5));
				searchBoxV.add(searchF = new JTextPane());
					searchF.setBorder(new JTextField().getBorder());
					searchF.setDocument(new SearchColorizingDocument(searchF, app));
					searchF.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent e) {
							if (!lastSearch.trim().equals(searchF.getText().trim())) {
								search(searchF.getText());
							}
						}
					});
				searchBoxV.add(Box.createVerticalStrut(10));
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(buttonP = new JPanel(new FlowLayout()), BorderLayout.EAST);
				buttonP.add(newDocumentB = new JButton("New Document"));
					newDocumentB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						searchF.requestFocusInWindow();
						newDocument();
					}});
		c.add(split = new JSplitPane(), BorderLayout.CENTER);
			split.setBorder(null);
			split.setLeftComponent(docsP = new JPanel(new BorderLayout(5, 5)));
				docsP.add(docsL = new JLabel("Documents"), BorderLayout.NORTH);
				docsP.add(docsListSP = new JScrollPane(), BorderLayout.CENTER);
					docsListSP.setViewportView(docsList = new JList(docsListM = new DocsListModel(this)));
						docsList.setCellRenderer(new DocCellRenderer(this));
						docsList.setFocusable(false);
			split.setRightComponent(tagsP = new JPanel(new BorderLayout(5, 5)));
				tagsP.add(tagsL = new JLabel("Tags"), BorderLayout.NORTH);
				tagsP.add(tagsListSP = new JScrollPane(), BorderLayout.CENTER);
					tagsListSP.setViewportView(tagsList = new JList(tagsListM = new TagsListModel(this)));
						tagsList.setCellRenderer(new TagCellRenderer(this));
						tagsList.addMouseListener(new MouseAdapter() {
							@Override
							public void mousePressed(MouseEvent e) {
								tagClick(tagsList.locationToIndex(e.getPoint()));
							}
						});
						tagsList.setFocusable(false);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				app.close();
				System.exit(0);
			}
		});
		pack();
		setSize(800, 600);
		split.setDividerLocation(500);
	}

	void search(String searchText) {
		lastSearch = searchText;
		String[] terms = searchText.split(" +");
		lastSearchKV.clear();
		lastSearchKeys.clear();
		suggestions.clear();
		for (TagSuggestor ts : suggestors) { suggestions.addAll(ts.suggestSearchTerms(terms)); }
		for (String t : terms) {
			String[] kv = t.split(":", 2);
			lastSearchKV.put(kv[0], kv.length == 1 ? null : kv[1]);
			lastSearchKeys.add(kv[0]);
		}
		currentSearchResult = app.getIndex().search(lastSearchKV);
		if (docsListM != null) { docsListM.changed(); }
		if (tagsListM != null) { tagsListM.changed(); }
	}

	void tagClick(int index) {
		if (index != -1) {
			String tag = (String) tagsListM.getElementAt(index);
			String[] tags = searchF.getText().split(" +");
			boolean tagAlreadyExists = false;
			for (String t : tags) {
				String key = t.split(":", 2)[0];
				if (key.equals(tag)) { tagAlreadyExists = true; break; }
			}
			if (tagAlreadyExists) {
				// Remove the tag.
				int searchStart = 0;
				int found = -1;
				String text = searchF.getText();
				while ((found = text.indexOf(tag, searchStart)) != -1) {
					// Check this is not just a substring of a larger tag.
					if (found != 0 && text.charAt(found - 1) != ' ') { continue; }
					int consumeToRight = 0;
					if (found + tag.length() < text.length()) {
						char nextC = text.charAt(found + tag.length());
						if (nextC == ' ') {
							consumeToRight = 1;
						} else {
							if (nextC == ':') {
								// The tag is a key/value pair. We want to get rid of both, so let's
								// see how far the value extends.
								int nextSpaceIndex = text.indexOf(" ", found + 1);
								if (nextSpaceIndex == -1) { nextSpaceIndex = text.length(); }
								consumeToRight = nextSpaceIndex - found - tag.length();
							} else {
								// The tag we've clicked on happens to be the start of a preexisting
								// tag.
								continue;
							}
						}
					}
					// OK, it's bounded on both sides by the end of the document or spaces.
					try {
						searchF.getDocument().remove(found, tag.length() + consumeToRight);
						search(searchF.getText());
					} catch (BadLocationException e) {
						// La la la should not happen.
					}
					searchStart = found + 1;
				}
			} else {
				// Insert the tag.
				try {
					// Put spaces around the tag as needed.
					if (searchF.getCaretPosition() != 0) {
						if (searchF.getText().charAt(searchF.getCaretPosition() - 1) != ' ') {
							tag = " " + tag;
						}
					}
					/*if (searchF.getCaretPosition() == searchF.getText().length() ||
						searchF.getText().charAt(searchF.getCaretPosition()) != ' ')
					{
						tag = tag + " ";
					}*/
					searchF.getDocument().insertString(searchF.getCaretPosition(), tag, null);
					search(searchF.getText());
				} catch (BadLocationException e) {
					// La la la should not happen.
				}
			}
		}
	}

	void newDocument() {
		// Creates a new document with some semiconvinging tags.
		Document d = app.getStore().create();
		Random r = new Random();
		
		d.put(new String[] { "bill", "letter", "topay", "notes" }[r.nextInt(3)], "");
		d.put("name", new String[] {"bob", "suzy", "mike"}[r.nextInt(3)]);
		d.put("d",
				(2005 + r.nextInt(6)) + "-" +
				"0" + (1 + r.nextInt(8)) + "-" +
				(10 + r.nextInt(18)));

		// Then re-search to include it in the view.
		search(lastSearch);
	}
}
