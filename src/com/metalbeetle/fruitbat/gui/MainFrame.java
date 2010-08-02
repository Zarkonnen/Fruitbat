package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Closeable;
import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.ProgressMonitor;
import com.metalbeetle.fruitbat.storage.SearchOutcome;
import com.metalbeetle.fruitbat.storage.SearchResult;
import com.metalbeetle.fruitbat.storage.Store;
import com.metalbeetle.fruitbat.storage.StoreConfig;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
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
import java.util.HashMap;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import static com.metalbeetle.fruitbat.util.Misc.*;

public class MainFrame extends JFrame implements Closeable {
	static final int DEFAULT_MAX_DOCS = 50;

	final Fruitbat app;
	final Store store;
	final StoreConfig config;

	ProgressMonitor pm;
	boolean isEmergencyShutdown = false;

	final OpenDocManager openDocManager = new OpenDocManager();
	final ShortcutOverlay shortcutOverlay = new ShortcutOverlay();

	SearchResult currentSearchResult;
	HashMap<String, String> lastSearchKV = new HashMap<String, String>();
	List<String> lastSearchKeys = new ArrayList<String>();
	String lastSearch = "";
	
	SearchTagCompleteMenu completeMenu = null;

	File lastDirectory = new File("");
	boolean tagsChanged = false;

	final MainMenuBar mainMenuBar;
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

	public MainFrame(Fruitbat application, Store store, ProgressMonitor pm, StoreConfig config) {
		super("Fruitbat: " + store);
		app = application;
		this.store = store;
		this.pm = pm;
		this.config = config;
		search("", DEFAULT_MAX_DOCS, /*force*/ true);

		Container c = getContentPane();
		c.setLayout(new BorderLayout(0, 5));
		final MainFrame self = this;
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
			split.setRightComponent(tagsP = new JPanel(new BorderLayout(5, 5)));
				tagsP.add(tagsL = new JLabel("with these tags"), BorderLayout.NORTH);
				tagsP.add(tagsListSP = new JScrollPane(), BorderLayout.CENTER);
					tagsListSP.setViewportView(tagsList = new NarrowSearchTagsList(this));
		setJMenuBar(mainMenuBar = new MainMenuBar(this));
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
					search(searchF.getText(), DEFAULT_MAX_DOCS, /*force*/true);
					tagsChanged = false;
				}
				searchF.requestFocusInWindow();
			}
		});
		shortcutOverlay.attachTo(this);
		pack();
		setSize(800, 700);
		split.setDividerLocation(500);
	}

	public StoreConfig getConfig() { return config; }

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

	void search() { search(searchF.getText(), DEFAULT_MAX_DOCS); }

	void search(String searchText, int maxDocs) { search(searchText, maxDocs, false); }

	void search(String searchText, final int maxDocs, boolean force) {
		try {
			lastSearch = searchText;
			String[] terms = searchText.split(" +");
			final HashMap<String, String> searchKV = new HashMap<String, String>();
			for (String t : terms) {
				String[] kv = t.split(":", 2);
				if (kv[0].length() == 0) { continue; }
				if (searchKV.containsKey(kv[0])) { continue; }
				if (!store.getIndex().isKey(kv[0])) { continue; }
				searchKV.put(kv[0], kv.length == 1 ? null : kv[1]);
			}
			if (force || !lastSearchKV.equals(searchKV)) {
				lastSearchKV = searchKV;
				lastSearchKeys.clear();
				lastSearchKeys.addAll(searchKV.keySet());
				currentSearchResult = store.getIndex().search(lastSearchKV, maxDocs);
				if (docsList != null) {
					docsList.m.changed();
					docsList.clearSelection();
					updateDocsLabel();
				}
				if (tagsList != null) {
					tagsList.m.changed();
				}
			}
		} catch (FatalStorageException e) {
			pm.handleException(e, this);
		}
	}

	void newDocument() {
		try {
			Document d = store.create();
			search(lastSearch, DEFAULT_MAX_DOCS, /*force*/ true);
			openDocManager.open(d, this);
		} catch (FatalStorageException e) {
			pm.handleException(e, this);
		}
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
}
