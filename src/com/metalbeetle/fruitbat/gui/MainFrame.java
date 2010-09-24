package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.ByValueComparator;
import com.metalbeetle.fruitbat.Closeable;
import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.DocIndex;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.EnhancedStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.SearchOutcome;
import com.metalbeetle.fruitbat.storage.SearchResult;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class MainFrame extends JFrame implements Closeable, FileDrop.Listener {
	static final int DEFAULT_MAX_DOCS = 50;

	final Fruitbat app;
	final EnhancedStore store;
	final StoreConfig config;
	boolean showDeletedDocs = false;

	ProgressMonitor pm;
	boolean isEmergencyShutdown = false;

	final OpenDocManager openDocManager = new OpenDocManager(this);
	
	SearchResult currentSearchResult;
	HashMap<String, String> lastSearchKV = new HashMap<String, String>();
	List<String> lastSearchKeys = new ArrayList<String>();
	String lastSearch = "";
	int lastMaxDocs = DEFAULT_MAX_DOCS;
	
	SearchTagCompleteMenu completeMenu = null;

	File lastDirectory = new File("");
	boolean tagsChanged = false;

	final MainMenuBar menuBar;
	final Box searchBoxH;
		final Box searchBoxV;
			final JTextPane searchF;
		final JPanel buttonP;
			final JButton undeleteDocumentB;
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

	public MainFrame(Fruitbat application, EnhancedStore store, ProgressMonitor pm,
			StoreConfig config)
	{
		super("Fruitbat: " + store);
		app = application;
		this.store = store;
		this.pm = pm;
		this.config = config;
		search("", DEFAULT_MAX_DOCS, /*force*/ true);

		Container c = getContentPane();
		c.setLayout(new BorderLayout(0, 5));
		// This layouting is horrible and should be replaced by a grid bag.
		c.add(searchBoxH = Box.createHorizontalBox(), BorderLayout.NORTH);
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(searchBoxV = Box.createVerticalBox());
				searchBoxV.add(Box.createVerticalStrut(5));
				searchBoxV.add(searchF = new FixedTextPane());
					searchF.setDocument(new SearchColorizingDocument(this));
					searchF.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent e) {
							if (!lastSearch.trim().equals(searchF.getText().trim())) {
								search();
							}
							if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
								switchCompleteMenu();
							}
						}
					});
				searchBoxV.add(Box.createVerticalStrut(5));
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(buttonP = new JPanel(new FlowLayout()));
				buttonP.add(undeleteDocumentB = new JButton("Undelete Document"));
					undeleteDocumentB.setFocusable(false);
					undeleteDocumentB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						searchF.requestFocusInWindow();
						undeleteSelectedDocument();
					}});
					undeleteDocumentB.setEnabled(false);
					undeleteDocumentB.setVisible(false);
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
					docsL.addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) {
						searchMore();
					}});
					docsL.setCursor(new Cursor(Cursor.HAND_CURSOR));
				docsP.add(docsListSP = new JScrollPane(), BorderLayout.CENTER);
					docsListSP.setViewportView(docsList = new DocsList(this));
					docsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
						public void valueChanged(ListSelectionEvent e) { updateDeleteAndUndeleteEnabled(); }
					});
					new FileDrop(docsListSP, this);
			split.setRightComponent(tagsP = new JPanel(new BorderLayout(5, 5)));
				tagsP.add(tagsL = new JLabel("with these tags"), BorderLayout.NORTH);
				tagsP.add(tagsListSP = new JScrollPane(), BorderLayout.CENTER);
					tagsListSP.setViewportView(tagsList = new NarrowSearchTagsList(this));
		updateDocsLabel();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
			@Override
			public void windowActivated(WindowEvent e) {
				if (tagsChanged) {
					((SearchColorizingDocument) searchF.getDocument()).colorize();
					search(searchF.getText(), DEFAULT_MAX_DOCS, /*force*/true);
					tagsChanged = false;
				}
			}
		});
		setJMenuBar(menuBar = new MainMenuBar(this));
		app.shortcutOverlay.attachTo(this);
		pack();
		setSize(800, 700);
		split.setDividerLocation(500);
	}

	public StoreConfig getConfig() { return config; }

	public void setShowDeletedDocs(boolean showDeletedDocs) {
		this.showDeletedDocs = showDeletedDocs;
		menuBar.undeleteMI.setVisible(showDeletedDocs);
		undeleteDocumentB.setVisible(showDeletedDocs);
		forceSearch();
		((SearchColorizingDocument) searchF.getDocument()).colorize();
	}

	void updateDeleteAndUndeleteEnabled() {
		try {
			boolean docsListFocus = docsList.hasFocus();
			int docIndex = docsList.getSelectedIndex();
			boolean enabled = docsList.getSelectedIndex() != -1 &&
					currentSearchResult.docs.get(docsList.getSelectedIndex()).
					has(Fruitbat.ALIVE_KEY);
			menuBar.deleteMI.setEnabled(enabled);
			boolean unEnabled = docsList.getSelectedIndex() != -1 &&
					currentSearchResult.docs.get(docsList.getSelectedIndex()).
					has(Fruitbat.DEAD_KEY);
			undeleteDocumentB.setEnabled(unEnabled);
			menuBar.undeleteMI.setEnabled(unEnabled);
			if (docsListFocus) {
				docsList.requestFocusInWindow();
			} else {
				searchF.requestFocusInWindow();
			}
			docsList.setSelectedIndex(docIndex);
		} catch (FatalStorageException e) {
			handleException(e);
		}
	}

	/** Call when closing application. */
	public void close() {
		openDocManager.close();
		try {
			store.close();
		} catch (Exception e) {
			pm.handleException(e, this);
		}
		app.storeClosed(this);
	}

	/** Show/hide a menu for completing a half-started tag. */
	void switchCompleteMenu() {
		if (completeMenu != null) {
			completeMenu.setVisible(false);
			completeMenu = null;
			return;
		}
		completeMenu = new SearchTagCompleteMenu(this);
		if (completeMenu.getSubElements().length > 0) {
			completeMenu.show(searchF, 0, searchF.getHeight());
		} else {
			completeMenu = null;
		}
	}

	void updateDocsLabel() {
		String s = "<html>" + currentSearchResult.docs.size() + " documents";
		if (currentSearchResult.outcome != SearchOutcome.EXHAUSTIVE) {
			s += " of \u2265" + currentSearchResult.minimumAvailableDocs +
					" <font color=\"#0000FF\"><u>more</u></font>";
		}
		s += "</html>";
		docsL.setText(s);
	}

	void searchMore() {
		int maxDocs = -1;
		if (currentSearchResult.minimumAvailableDocs < 400) {
			maxDocs = currentSearchResult.minimumAvailableDocs;
		} else {
			String answer = pm.askQuestion("More...",
					"How many documents would you like to see?",
					string(Math.min(currentSearchResult.minimumAvailableDocs,
					1000)));
			try {
				maxDocs = Integer.parseInt(answer);
			} catch (Exception ex) {}
		}
		if (maxDocs > 0) {
			search(searchF.getText(), maxDocs, /*force*/ true);
		}
	}

	DocIndex getIndex() { return store.getIndex(); }

	void search() { search(searchF.getText(), DEFAULT_MAX_DOCS); }
	
	void search(String searchText, int maxDocs) { search(searchText, maxDocs, false); }

	void forceSearch() { search(searchF.getText(), DEFAULT_MAX_DOCS, true); }

	/** @return Whether the given string is a key that will give results in search. */
	boolean isKey(String s) throws FatalStorageException {
		return getIndex().isKey(s) &&
				(showDeletedDocs ||
				getIndex().search(
						m(
								p(s, (String) null),
								p(Fruitbat.ALIVE_KEY, (String) null)
						),
						1).docs.size() > 0
				);
	}

	void search(String searchText, final int maxDocs, boolean force) {
		try {
			lastSearch = searchText;
			lastMaxDocs = maxDocs;
			String[] terms = searchText.split(" +");
			final HashMap<String, String> searchKV = new HashMap<String, String>();
			final ArrayList<String> sortKeys = new ArrayList<String>();
			for (String t : terms) {
				String[] kv = t.split(":", 2);
				if (kv[0].length() == 0) { continue; }
				if (searchKV.containsKey(kv[0])) { continue; }
				if (!isKey(kv[0])) { continue; }
				searchKV.put(kv[0], kv.length == 1 ? null : kv[1]);
				sortKeys.add(kv[0]);
			}
			if (!showDeletedDocs) { searchKV.put(Fruitbat.ALIVE_KEY, null); }
			if (force || !lastSearchKV.equals(searchKV)) {
				lastSearchKV = searchKV;
				lastSearchKeys.clear();
				lastSearchKeys.addAll(searchKV.keySet());
				lastSearchKeys.remove(Fruitbat.ALIVE_KEY);
				currentSearchResult = getIndex().search(lastSearchKV, maxDocs);
				Collections.sort(currentSearchResult.docs, new ByValueComparator(sortKeys));
				if (docsList != null) {
					docsList.m.changed();
					docsList.clearSelection();
					updateDocsLabel();
				}
				if (tagsList != null) {
					tagsList.m.changed();
				}
				if (menuBar != null) {
					updateDeleteAndUndeleteEnabled();
				}
			}
		} catch (FatalStorageException e) {
			pm.handleException(e, this);
		}
	}

	DocumentFrame newDocument() {
		try {
			Document d = store.create();
			search(lastSearch, DEFAULT_MAX_DOCS, /*force*/ true);
			return openDocManager.open(d);
		} catch (FatalStorageException e) {
			pm.handleException(e, this);
			return null;
		}
	}

	void deleteSelectedDocument() {
		if (docsList.getSelectedIndex() == -1) { return; }
		int docIndex = docsList.getSelectedIndex();
		Document d = currentSearchResult.docs.get(docsList.getSelectedIndex());
		DocumentFrame df = openDocManager.getAndToFrontIfOpen(d);
		if (df == null) {
			try {
				store.delete(d);
				forceSearch();
			} catch (Exception e) {
				handleException(e);
			}
		} else {
			df.delete();
		}
		docsList.requestFocusInWindow();
		docsList.setSelectedIndex(docIndex);
	}

	DocumentFrame undeleteSelectedDocument() {
		if (docsList.getSelectedIndex() == -1) { return null; }
		try {
			int docIndex = docsList.getSelectedIndex();
			DocumentFrame df = undeleteDocument(
					currentSearchResult.docs.get(docsList.getSelectedIndex()).getID());
			docsList.requestFocusInWindow();
			docsList.setSelectedIndex(docIndex);
			return df;
		} catch (Exception e) {
			pm.handleException(new FatalStorageException("Could not undelete document.", e), null);
			return null;
		}
	}

	DocumentFrame undeleteDocument(int id) {
		try {
			Document d = store.undelete(store.get(id));
			DocumentFrame df = openDocManager.getAndToFrontIfOpen(d);
			if (df != null) { df.updateIsDeletedStatus(); }
			forceSearch();
			return df;
		} catch (Exception e) {
			pm.handleException(new FatalStorageException("Could not undelete document.", e),
					this);
			return null;
		}
	}

	public void filesDropped(File[] files) {
		newDocument().filesDropped(files);
	}

	public void setProgressMonitor(ProgressMonitor pm) {
		this.pm = pm;
		store.setProgressMonitor(pm);
	}

	void handleException(Exception e) {
		pm.handleException(e, this);
	}

	void setIsEmergencyShutdown() {
		isEmergencyShutdown = true;
	}

	public void writePrefs(Preferences p) throws BackingStoreException, FatalStorageException {
		if (isVisible()) {
			p.putInt("x", getX());
			p.putInt("y", getY());
			p.putInt("width", getWidth());
			p.putInt("height", getHeight());
			p.putBoolean("focused", isFocused());
			p.put("searchTerms", searchF.getText());
			p.putInt("searchCaret", searchF.getCaretPosition());
			p.putInt("maxDocs", lastMaxDocs);
			p.putInt("selectedDoc", docsList.getSelectedIndex());
			p.putInt("docScrollX", docsListSP.getViewport().getViewPosition().x);
			p.putInt("docScrollY", docsListSP.getViewport().getViewPosition().y);
			p.putInt("tagScrollX", tagsListSP.getViewport().getViewPosition().x);
			p.putInt("tagScrollY", tagsListSP.getViewport().getViewPosition().y);
			p.putBoolean("docListFocused", docsList.isFocusOwner() || docsListSP.isFocusOwner());
			p.putBoolean("showDeletedDocs", showDeletedDocs);
		}
		openDocManager.writePrefs(p.node("openDocs"));
		p.flush();
	}

	public void readPrefs(final Preferences p) throws BackingStoreException, FatalStorageException {
		setLocation(p.getInt("x", getX()), p.getInt("y", getY()));
		setSize(p.getInt("width", getWidth()), p.getInt("height", getHeight()));
		searchF.setText(p.get("searchTerms", ""));
		search(p.get("searchTerms", ""), p.getInt("maxDocs", DEFAULT_MAX_DOCS), /*force*/ true);
		searchF.setCaretPosition(p.getInt("searchCaret", searchF.getText().length()));
		try { docsList.setSelectedIndex(p.getInt("selectedDoc", -1)); } catch (Exception e) {}
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			docsListSP.getViewport().setViewPosition(
					new Point(p.getInt("docScrollX", 0), p.getInt("docScrollY", 0)));
			tagsListSP.getViewport().setViewPosition(
					new Point(p.getInt("tagScrollX", 0), p.getInt("tagScrollY", 0)));
		}});
		openDocManager.readPrefs(p.node("openDocs"));
		if (p.getBoolean("focused", false)) {
			toFront();
		}
		if (p.getBoolean("docListFocused", false)) {
			docsList.requestFocusInWindow();
		} else {
			searchF.requestFocusInWindow();
		}
		setShowDeletedDocs(p.getBoolean("showDeletedDocs", false));
	}
}
