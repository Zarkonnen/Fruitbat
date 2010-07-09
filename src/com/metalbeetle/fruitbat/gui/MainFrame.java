package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.util.Pair;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.gui.Colors.*;

public class MainFrame extends JFrame {
	final Fruitbat app;

	final OpenDocManager openDocManager = new OpenDocManager();

	List<TagSuggestor> suggestors;
	List<String> suggestions = new ArrayList<String>();

	Pair<List<Document>, List<String>> currentSearchResult;
	HashMap<String, String> lastSearchKV = new HashMap<String, String>();
	List<String> lastSearchKeys = new ArrayList<String>();
	String lastSearch = "";

	Document quickLookD = null;

	TagCompleteMenu completeMenu = null;

	final Box searchBoxH;
		final Box searchBoxV;
			final JTextPane searchF;
		final JPanel buttonP;
			final JButton newDocumentB;
	final JSplitPane split;
		final JPanel docsP;
			final JLabel docsL;
			final JScrollPane docsListSP;
				final DocsList docsList;
		final JPanel tagsP;
			final JLabel tagsL;
			final JScrollPane tagsListSP;
				final NarrowSearchTagsList tagsList;
				final JLabel quickLookF;

	public MainFrame(Fruitbat application) throws HeadlessException {
		super("Fruitbat");
		app = application;
		suggestors = l((TagSuggestor) new DateSuggestor());
		search("", /*force*/ true);

		Container c = getContentPane();
		c.setLayout(new BorderLayout(0, 5));
		// This layouting is horrible and should be replaced by a grid bag.
		c.add(searchBoxH = Box.createHorizontalBox(), BorderLayout.NORTH);
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(searchBoxV = Box.createVerticalBox());
				searchBoxV.add(Box.createVerticalStrut(5));
				searchBoxV.add(searchF = new JTextPane());
					// Fix tab focusing behaviour.
					Set<KeyStroke> strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("pressed TAB")));
					searchF.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, strokes);
					strokes = new HashSet<KeyStroke>(Arrays.asList(KeyStroke.getKeyStroke("shift pressed TAB")));
					searchF.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, strokes);
					searchF.setBorder(new JTextField().getBorder());
					searchF.setDocument(new SearchColorizingDocument(searchF, app));
					searchF.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent e) {
							if (!lastSearch.trim().equals(searchF.getText().trim())) {
								search(searchF.getText());
							}
							if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
								switchCompleteMenu();
							}
						}
					});
				searchBoxV.add(Box.createVerticalStrut(5));
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(buttonP = new JPanel(new FlowLayout()));
				buttonP.add(newDocumentB = new JButton("New Document"));
					newDocumentB.setFocusable(false);
					newDocumentB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						searchF.requestFocusInWindow();
						newDocument();
					}});
		c.add(split = new JSplitPane(), BorderLayout.CENTER);
			split.setBorder(new EmptyBorder(0, 5, 5, 5));
			split.setLeftComponent(docsP = new JPanel(new BorderLayout(5, 5)));
				docsP.add(docsL = new JLabel("Documents"), BorderLayout.NORTH);
					docsL.addMouseListener(new MouseAdapter() {
						@Override
						public void mousePressed(MouseEvent e) {
							docsList.m.setShowAll(!docsList.m.getShowAll());
							updateDocsLabel();
						}
					});
					docsL.setCursor(new Cursor(Cursor.HAND_CURSOR));
				docsP.add(docsListSP = new JScrollPane(), BorderLayout.CENTER);
					docsListSP.setViewportView(docsList = new DocsList(this));
			split.setRightComponent(tagsP = new JPanel(new BorderLayout(5, 5)));
				tagsP.add(tagsL = new JLabel("Tags"), BorderLayout.NORTH);
				tagsP.add(tagsListSP = new JScrollPane(), BorderLayout.CENTER);
					tagsListSP.setViewportView(tagsList = new NarrowSearchTagsList(this));
			quickLookF = new JLabel();
				quickLookF.setBorder(new EmptyBorder(3, 3, 3, 3));
				quickLookF.setVerticalAlignment(SwingConstants.TOP);
				quickLookF.setBackground(TAG_BG);
				quickLookF.setOpaque(true);
		updateDocsLabel();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		pack();
		setSize(800, 600);
		split.setDividerLocation(500);
	}

	/** Show/hide a menu for completing a half-started tag. */
	void switchCompleteMenu() {
		if (completeMenu != null) {
			completeMenu.setVisible(false);
			completeMenu = null;
			return;
		}
		completeMenu = new TagCompleteMenu(this);
		if (completeMenu.getSubElements().length > 0) {
			completeMenu.show(searchF, 0, searchF.getHeight());
		} else {
			completeMenu = null;
		}
	}

	void updateDocsLabel() {
		String s = "<html>" + currentSearchResult.a.size() + " documents";
		if (docsList.m.getShowAll()) {
			s += " ( <font color=\"#0000FF\"><u>show " + DocsListModel.MAX_DOCS_DISPLAYED +
					" only</u></font> )";
		} else {
			if (currentSearchResult.a.size() != docsList.m.getSize()) {
				s += ", " + docsList.m.getSize() +
						" shown ( <font color=\"#0000FF\"><u>show all</u></font> )";
			}
		}
		s += "</html>";
		docsL.setText(s);
	}

	void search(String searchText) { search(searchText, false); }

	void search(String searchText, boolean force) {
		lastSearch = searchText;
		String[] terms = searchText.split(" +");
		HashMap<String, String> searchKV = new HashMap<String, String>();
		for (String t : terms) {
			String[] kv = t.split(":", 2);
			if (searchKV.containsKey(kv[0])) { continue; }
			if (!app.getIndex().isKey(kv[0])) { continue; }
			searchKV.put(kv[0], kv.length == 1 ? null : kv[1]);
		}
		if (force || !lastSearchKV.equals(searchKV)) {
			lastSearchKV = searchKV;
			lastSearchKeys.clear();
			lastSearchKeys.addAll(searchKV.keySet());
			suggestions.clear();
			for (TagSuggestor ts : suggestors) { suggestions.addAll(ts.suggestSearchTerms(terms)); }
			currentSearchResult = app.getIndex().search(lastSearchKV);
			if (docsList != null) {
				docsList.m.setShowAll(false);
				docsList.clearSelection();
				updateDocsLabel();
			}
			if (tagsList != null) {
				tagsList.m.changed();
			}
		}
	}

	void quickLook(int index) {
		Document d = index == -1 ? null : currentSearchResult.a.get(index);
		if (d != quickLookD) {
			quickLook(d);
		}
	}

	void quickLook(Document d) {
		quickLookD = d;
		if (d == null) {
			tagsL.setText("Tags");
			tagsListSP.setViewportView(tagsList);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("<html>");
			for (String k : d.keys()) {
				sb.append("<font color=\"");
				sb.append(TAG_HTML);
				sb.append("\">");
				sb.append(k);
				sb.append("</font>");
				String v = d.get(k);
				if (v.length() > 0) {
					sb.append("<font color=\"");
					sb.append(VALUE_HTML);
					sb.append("\">:");
					sb.append(v);
					sb.append("</font>");
				}
				sb.append("<br>");
			}
			sb.append("</html>");
			quickLookF.setText(sb.toString());
			tagsL.setText("Tags for this document");
			tagsListSP.setViewportView(quickLookF);
		}
	}

	void newDocument() {
		// Creates a new document with some semiconvincing tags.
		Document d = app.getStore().create();
		Random r = new Random();
		
		d.put(new String[] { "bill", "letter", "topay", "notes" }[r.nextInt(3)], "");
		d.put(new String[] {"bob", "suzy", "mike"}[r.nextInt(3)], "");
		d.put("d",
				(2005 + r.nextInt(6)) + "-" +
				"0" + (1 + r.nextInt(8)) + "-" +
				(10 + r.nextInt(18)));

		// Then re-search to include it in the view.
		search(lastSearch, /*force*/ true);
		openDocManager.open(d, this);
	}
}
