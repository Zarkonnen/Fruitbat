package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.BlockingTask;
import javax.swing.text.BadLocationException;
import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.io.StringSrc;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.DocumentTools;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.PageChange;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

class DocumentFrame extends JFrame implements FileDrop.Listener {
	static final List<String> ACCEPTED_EXTENSIONS = l(".jpg", ".tiff", ".tif", ".bmp", ".png",
			".pdf");

	final Document d;
	final StoreFrame sf;
	final DocumentMenuBar menuBar;
	InputTagCompleteMenu completeMenu;
	boolean tagsChanged = false;
	boolean notesChanged = false;
	final Caret tagsFCaret;
	boolean deletedPageMode = false;

	final Box searchBoxH;
		final Box searchBoxV;
			final JTextPane tagsF;
				final TagColorizingDocument tagsD;
		final JPanel buttonP;
			final JButton addPageB;
	final JSplitPane hSplit;
		final JSplitPane split;
			final JSplitPane tagSplit;
				final JPanel suggestedTagsP;
					final JLabel suggestedTagsL;
					final JScrollPane suggestedTagsListSP;
						final SuggestedTagsList suggestedTagsList;
				final JPanel allTagsP;
					final JLabel allTagsL;
					final JScrollPane allTagsListSP;
						final AllTagsList allTagsList;
			final PagesViewer viewer;
		final JScrollPane notesSP;
			final FixedTextPane notesPane;

	public DocumentFrame(final Document d, final StoreFrame sf) throws HeadlessException {
		super("Fruitbat Document ");
		this.d = d;
		this.sf = sf;

		setJMenuBar(menuBar = new DocumentMenuBar(this));

		Container c = getContentPane();
		c.setLayout(new BorderLayout(0, 5));
		// This layouting is horrible and should be replaced by a grid bag.
		c.add(searchBoxH = Box.createHorizontalBox(), BorderLayout.NORTH);
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(searchBoxV = Box.createVerticalBox());
				searchBoxV.add(Box.createVerticalStrut(5));
				searchBoxV.add(tagsF = new FixedTextPane());
					tagsFCaret = tagsF.getCaret();
					tagsF.setDocument(tagsD = new TagColorizingDocument(tagsF));
					tagsF.getDocument().addUndoableEditListener(new WindowExpirationWrapper(this, sf.app.undoManager));
					tagsF.getDocument().addDocumentListener(new DocumentListener() {
						public void insertUpdate(DocumentEvent e) {
							allTagsList.update();
							suggestedTagsList.update();
							tagsChanged = true;
						}
						public void removeUpdate(DocumentEvent e) {
							allTagsList.update();
							suggestedTagsList.update();
							tagsChanged = true;
						}
						public void changedUpdate(DocumentEvent e) {
							allTagsList.update();
							suggestedTagsList.update();
							tagsChanged = true;
						}
					});
					tagsF.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent e) {
							if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
								switchCompleteMenu();
							}
						}
					});
				searchBoxV.add(Box.createVerticalStrut(5));
			searchBoxH.add(Box.createHorizontalStrut(5));
			searchBoxH.add(buttonP = new JPanel(new FlowLayout()));
				buttonP.add(addPageB = new JButton("Add Page"));
					addPageB.setFocusable(false);
					addPageB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
						addPage();
					}});
		c.add(hSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT), BorderLayout.CENTER);
			hSplit.setTopComponent(split = new JSplitPane());
				split.setBorder(new EmptyBorder(0, 5, 5, 5));
					split.setLeftComponent(tagSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT));
						tagSplit.setBorder(new EmptyBorder(0, 5, 5, 5));
						tagSplit.setTopComponent(suggestedTagsP = new JPanel(new BorderLayout(5, 5)));
							suggestedTagsP.add(suggestedTagsL = new JLabel("Suggested tags"), BorderLayout.NORTH);
							suggestedTagsP.add(suggestedTagsListSP = new JScrollPane(), BorderLayout.CENTER);
								suggestedTagsListSP.setViewportView(suggestedTagsList = new SuggestedTagsList(this));
						tagSplit.setBottomComponent(allTagsP = new JPanel(new BorderLayout(5, 5)));
							allTagsP.add(allTagsL = new JLabel("All tags"), BorderLayout.NORTH);
							allTagsP.add(allTagsListSP = new JScrollPane(), BorderLayout.CENTER);
								allTagsListSP.setViewportView(allTagsList = new AllTagsList(this));
						tagSplit.setDividerLocation(200);
					split.setRightComponent(viewer = new PagesViewer(this));
					split.setDividerLocation(200);
			hSplit.setBottomComponent(notesSP = new JScrollPane());
				notesSP.setViewportView(notesPane = new FixedTextPane());
					notesPane.setToolTipText("Notes");
					notesPane.setBackground(Colors.NOTES_BG);
					notesPane.getDocument().addUndoableEditListener(new WindowExpirationWrapper(this, sf.app.undoManager));
					notesPane.getDocument().addDocumentListener(new DocumentListener() {
						public void insertUpdate(DocumentEvent de) { notesChanged = true; }
						public void removeUpdate(DocumentEvent de) { notesChanged = true; }
						public void changedUpdate(DocumentEvent de) { notesChanged = true; }
					});
			hSplit.setBorder(null);
			hSplit.setDividerLocation(600);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) { close(); }
			@Override
			public void windowIconified(WindowEvent e) { saveTagsAndNotes(); }
			@Override
			public void windowDeactivated(WindowEvent e) { saveTagsAndNotes(); }
		});

		new FileDrop(viewer, this);

		updateTagsAndNotes();
		updateDisplay();
		tagsChanged = false;
		sf.app.shortcutOverlay.attachTo(this);
		pack();
		setSize(800, 800);
	}

	boolean isDeleted() {
		try {
			return d.has(Fruitbat.DEAD_KEY);
		} catch (FatalStorageException e) {
			sf.handleException(e);
			return false;
		}
	}

	void close() {
		saveTagsAndNotes();
		sf.openDocManager.close(d);
	}

	/**
	 * Updates the content/visibility/enabledness of GUI elements depending on if the document is
	 * deleted, is showing deleted pages, etc.
	 */
	public void updateDisplay() {
		boolean isDeleted = isDeleted();

		setTitle(isDeleted ? "Fruitbat Document (Deleted)" : "Fruitbat Document");
		
		tagsF.setEditable(!isDeleted);
		tagsF.setCaret(tagsFCaret);
		tagsF.setCaretPosition(tagsF.getText().length());
		addPageB.setEnabled(!isDeleted);
		suggestedTagsList.setEnabled(!isDeleted);
		allTagsList.setEnabled(!isDeleted);
		
		// Ensure menu items have correct state.
		menuBar.undeleteMI.setEnabled(isDeleted);
		menuBar.deleteMI.setEnabled(!isDeleted);
		menuBar.addPageMI.setEnabled(!isDeleted);
		menuBar.insertPageBeforeMI.setEnabled(!isDeleted);
		menuBar.insertPageAfterMI.setEnabled(!isDeleted);
		viewer.updateDisplay();
	}

	void delete() {
		try {
			sf.store.delete(d);
			sf.tagsChanged = true;
			close();
			dispose();
		} catch (FatalStorageException e) {
			sf.handleException(e);
		}
	}

	/** Show/hide a menu for completing a half-started tag. */
	void switchCompleteMenu() {
		if (isDeleted()) { return; }
		if (completeMenu != null) {
			completeMenu.setVisible(false);
			completeMenu = null;
			return;
		}
		int xOffset = 0;
		try {
			xOffset = tagsF.modelToView(tagsF.getCaretPosition()).x;
		} catch (BadLocationException e) {
			// Not important, just default xOffset to 0.
		}
		completeMenu = new InputTagCompleteMenu(this);
		if (completeMenu.getSubElements().length > 0) {
			completeMenu.show(tagsF, xOffset, tagsF.getHeight());
		} else {
			completeMenu = null;
		}
	}

	void saveTagsAndNotes() {
		if (isDeleted()) { return; }
		try {
			if (tagsChanged && !sf.isEmergencyShutdown) {
				// Generate a mapping of tags.
				String[] terms = tagsF.getText().split(" +");
				HashMap<String, String> tags = new HashMap<String, String>();
				for (String t : terms) {
					String[] kv = t.split(":", 2);
					if (kv[0].length() == 0) { continue; }
					if (tags.containsKey(kv[0])) { continue; }
					tags.put(kv[0], kv.length == 1 ? "" : kv[1]);
				}

				// Compare them to the document's current state and create a list of changes.
				List<Change> changes = new ArrayList<Change>();
				for (Entry<String, String> kv : tags.entrySet()) {
					if (!(d.has(kv.getKey()) && d.get(kv.getKey()).equals(kv.getValue()))) {
						changes.add(DataChange.put(kv.getKey(), kv.getValue()));
					}
				}
				for (String dKey : d.keys()) {
					if (!dKey.startsWith(Fruitbat.HIDDEN_KEY_PREFIX) && !tags.containsKey(dKey)) {
						changes.add(DataChange.remove(dKey));
					}
				}

				if (changes.size() > 0) {
					d.change(changes);
				}
			}
			if (notesChanged && !sf.isEmergencyShutdown) {
				d.change(l(PageChange.put(DocumentTools.NOTES_KEY, new StringSrc(notesPane.getText()))));
			}
			if (tagsChanged) {
				sf.tagsChanged = true;
				tagsChanged = false;
			}
			if (notesChanged) {
				sf.tagsChanged = true;
				notesChanged = false;
			}
		} catch (FatalStorageException e) {
			sf.handleException(e);
		}
	}

	void updateTagsAndNotes() {
		try {
			tagsD.setUndosEnabled(false);
			if (d.keys().isEmpty()) {
				tagsF.setText("");
			}
			StringBuilder sb = new StringBuilder();
			for (String k : d.keys()) {
				if (k.startsWith(Fruitbat.HIDDEN_KEY_PREFIX)) {
					continue;
				}
				sb.append(k);
				String v = d.get(k);
				if (v.length() > 0) {
					sb.append(":");
					sb.append(v);
				}
				sb.append(" ");
			}
			tagsF.setText(sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1));
			tagsF.setCaretPosition(tagsF.getText().length());
			tagsD.setUndosEnabled(true);


			try {
				notesPane.getDocument().removeUndoableEditListener(new WindowExpirationWrapper(this, sf.app.undoManager));
				if (d.hasPage(DocumentTools.NOTES_KEY)) {
					notesPane.setText(srcToString(d.getPage(DocumentTools.NOTES_KEY)));
				} else {
					notesPane.setText("");
				}
			} catch (IOException e) {
				throw new FatalStorageException("Could not load notes.", e);
			} finally {
				notesPane.getDocument().addUndoableEditListener(new WindowExpirationWrapper(this, sf.app.undoManager));
			}
			notesChanged = false;
		} catch (FatalStorageException e) {
			sf.handleException(e);
		}
	}

	void insertPages(final File[] pageFiles, final boolean retainOriginals,
			final boolean deleteAfterAdding, final int atIndex)
	{
		final int numPages = pageFiles.length;
		saveTagsAndNotes();
		sf.pm.runBlockingTask("Inserting pages", new BlockingTask() {
			public boolean run() {
				return DocumentTools.insertPages(d, sf.store, sf.pm, pageFiles, retainOriginals,
						deleteAfterAdding, atIndex, numPages);
			}

			public void onSuccess() {
				viewer.setPage(Math.min(atIndex, numPages() - 1));
				suggestedTagsList.update();
			}

			public void onFailure() {
				repaint();
				suggestedTagsList.update();
			}
		});
	}

	public void moveCurrentPage() {
		final int currentIndex = viewer.getPage();
		final int numPages = numPages();
		int newIndex = currentIndex;
		String newPageNS = sf.pm.askQuestion("New location", "Where do you want to move this page?",
				string(newIndex + 1));
		try {
			newIndex = integer(newPageNS) - 1;
		} catch (Exception e) { /* meh */ }
		if (newIndex < 0) { newIndex = 0; }
		if (newIndex > numPages - 1) { newIndex = numPages - 1; }
		if (newIndex != currentIndex) {
			try {
				DocumentTools.movePage(d, sf.store, sf.pm, currentIndex, newIndex);
				viewer.setPage(newIndex);
			} catch (FatalStorageException e) {
				sf.handleException(e);
			}
		}
	}

	public void deleteCurrentPage() {
		try {
			if (!deletedPageMode && viewer.validPage()) {
				final int pageNum = viewer.getPage();
				DocumentTools.deletePage(d, sf.store, sf.pm, pageNum);
				int gotoPageNum = pageNum - 1;
				if (gotoPageNum == -1 && numPages() > 1) { gotoPageNum = 0; }
				viewer.setPage(gotoPageNum);
			}
		} catch (Exception e) {
			sf.handleException(e);
		}
	}

	public void undeleteCurrentPage() {
		try {
			if (deletedPageMode && viewer.validPage()) {
				final int pageNum = viewer.getPage();
				DocumentTools.undeletePage(d, sf.store, sf.pm, pageNum);
				int gotoPageNum = pageNum - 1;
				if (gotoPageNum == -1 && numPages() > 1) { gotoPageNum = 0; }
				viewer.setPage(gotoPageNum);
			}
		} catch (Exception e) {
			sf.handleException(e);
		}
	}

	public void addPage() {
		insertPagesAt(numPages());
		tagsF.requestFocusInWindow();
	}

	void insertPagesAt(int atIndex) {
		JFileChooser c = new JFileChooser(sf.lastDirectory);
		c.setDialogTitle("Choose one or several pages to " +
				(atIndex == numPages() ? "add" : "insert"));
		c.setAcceptAllFileFilterUsed(false);
		c.setFileFilter(new PageFileFilter());
		c.setMultiSelectionEnabled(true);
		ImportFileAccessory ifa = new ImportFileAccessory();
		c.setAccessory(ifa);
		if (c.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			sf.lastDirectory = c.getCurrentDirectory();
			File[] fs = c.getSelectedFiles();
			if (fs != null && fs.length > 0) {
				insertPages(fs, ifa.retainedOriginal.isSelected(), !ifa.storeCopy.isSelected(),
						atIndex);
			}
		}
	}

	/**
	 * Called when one or several files are dropped into the viewer. Recursively explores
	 * directories.
	 */
	public void filesDropped(File[] files) {
		if (isDeleted()) { return; }
		ArrayList<File> fs = new ArrayList<File>();
		for (File f : files) { fs.addAll(getAvailableFiles(f, ACCEPTED_EXTENSIONS, 0)); }
		if (fs.size() > 0) {
			insertPages(fs.toArray(new File[fs.size()]), false, false, numPages());
		}
	}

	String pagePrefix() { return deletedPageMode ? DocumentTools.DELETED_PREFIX : DocumentTools.NOT_DELETED_PREFIX; }

	int numPages() {
		try {
			return DocumentTools.numPagesFor(d, pagePrefix());
		} catch (FatalStorageException e) {
			sf.handleException(e);
			return -1;
		}
	}

	public void setShowDeletedPages(boolean deletedPageMode) {
		this.deletedPageMode = deletedPageMode;
		updateDisplay();
	}

	public void writePrefs(Preferences p) throws BackingStoreException, FatalStorageException {
		if (isVisible()) {
			p.putInt("x", getX());
			p.putInt("y", getY());
			p.putInt("width", getWidth());
			p.putInt("height", getHeight());
			p.putBoolean("focused", isFocused());
			p.putInt("pageNum", viewer.getPage());
			p.putInt("altPageNum", viewer.otherPageListIndex);
			p.putBoolean("deletedPageMode", deletedPageMode);
			p.putInt("suggestedTagsScrollX", suggestedTagsListSP.getViewport().getViewPosition().x);
			p.putInt("suggestedTagsScrollY", suggestedTagsListSP.getViewport().getViewPosition().y);
			p.putInt("allTagsScrollX", allTagsListSP.getViewport().getViewPosition().x);
			p.putInt("allTagsScrollY", allTagsListSP.getViewport().getViewPosition().y);
			p.putInt("splitDivider", split.getDividerLocation());
			p.putInt("hSplitDivider", hSplit.getDividerLocation());
			p.putInt("notesScrollX", notesSP.getViewport().getViewPosition().x);
			p.putInt("notesScrollY", notesSP.getViewport().getViewPosition().y);
		}
	}

	public void readPrefs(final Preferences p) throws BackingStoreException, FatalStorageException {
		setLocation(p.getInt("x", getX()), p.getInt("y", getY()));
		setSize(p.getInt("width", getWidth()), p.getInt("height", getHeight()));
		setShowDeletedPages(p.getBoolean("deletedPageMode", false));
		viewer.setPage(p.getInt("pageNum", 0));
		viewer.otherPageListIndex = p.getInt("altPageNum", -1);
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			suggestedTagsListSP.getViewport().setViewPosition(
					new Point(p.getInt("suggestedTagsScrollX", 0), p.getInt("suggestedTagsScrollY", 0)));
			allTagsListSP.getViewport().setViewPosition(
					new Point(p.getInt("allTagsScrollX", 0), p.getInt("allTagsScrollY", 0)));
			notesSP.getViewport().setViewPosition(
					new Point(p.getInt("notesScrollX", 0), p.getInt("notesScrollY", 0)));
			split.setDividerLocation(p.getInt("splitDivider", 200));
			hSplit.setDividerLocation(p.getInt("hSplitDivider", 600));
		}});
	}
}
