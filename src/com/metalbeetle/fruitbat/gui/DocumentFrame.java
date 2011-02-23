package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.gui.blockable.Blockable;
import javax.swing.text.BadLocationException;
import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.fulltext.FullTextExtractor;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.io.FileSrc;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.storage.PageChange;
import com.metalbeetle.fruitbat.util.ColorProfiler;
import com.metalbeetle.fruitbat.util.PreviewImager;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
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
	static final String PREVIEW_PREFIX = "p";
	static final String COLOR_PROFILE_1 = Fruitbat.HIDDEN_KEY_PREFIX + "cprof1";
	static final String COLOR_PROFILE_2 = Fruitbat.HIDDEN_KEY_PREFIX + "cprof2";
	static final String HARDCOPY_NUMBER_PREFIX = Fruitbat.HIDDEN_KEY_PREFIX + "ret";
	static final String FULLTEXT_PREFIX = Fruitbat.HIDDEN_KEY_PREFIX + "ft";
	static final String TMP_MOVE_PAGE_INDEX = "tmp";
	static final String DELETED_PREFIX = "d";
	static final String NOT_DELETED_PREFIX = "";

	final Document d;
	final MainFrame mf;
	final DocumentMenuBar menuBar;
	InputTagCompleteMenu completeMenu;
	boolean tagsChanged = false;
	final Caret tagsFCaret;
	boolean deletedPageMode = false;
	boolean blockUIInput = false;

	final Box searchBoxH;
		final Box searchBoxV;
			final JTextPane tagsF;
		final JPanel buttonP;
			final JButton addPageB;
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

	public DocumentFrame(final Document d, final MainFrame mf) throws HeadlessException {
		super("Fruitbat Document ");
		this.d = d;
		this.mf = mf;

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
					tagsF.setDocument(new TagColorizingDocument(tagsF));
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
		c.add(split = new JSplitPane(), BorderLayout.CENTER);
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

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) { if (!blockUIInput) { close(); } }
			@Override
			public void windowIconified(WindowEvent e) { if (!blockUIInput) { saveTags(); } }
			@Override
			public void windowDeactivated(WindowEvent e) { if (!blockUIInput) { saveTags(); } }
		});

		new FileDrop(viewer, this);

		updateTags();
		updateDisplay();
		tagsChanged = false;
		mf.app.shortcutOverlay.attachTo(this);
		pack();
		setSize(800, 800);
	}

	boolean isDeleted() {
		try {
			return d.has(Fruitbat.DEAD_KEY);
		} catch (FatalStorageException e) {
			mf.handleException(e);
			return false;
		}
	}

	void close() {
		saveTags();
		mf.openDocManager.close(d);
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
			mf.store.delete(d);
			mf.tagsChanged = true;
			close();
			dispose();
		} catch (FatalStorageException e) {
			mf.handleException(e);
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

	void saveTags() {
		if (isDeleted()) { return; }
		try {
			if (tagsChanged && !mf.isEmergencyShutdown) {
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
			if (tagsChanged) {
				mf.tagsChanged = true;
				tagsChanged = false;
			}
		} catch (FatalStorageException e) {
			mf.handleException(e);
		}
	}

	void updateTags() {
		try {
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
		} catch (FatalStorageException e) {
			mf.handleException(e);
		}
	}

	void setBlockUIInput(final boolean blockUIInput) {
		final DocumentFrame self = this;
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					if (!self.blockUIInput && blockUIInput) {
						setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
						Blockable.setBlocked(getJMenuBar(), true);
						setGlassPane(new AllInterceptingPane());
						getGlassPane().setVisible(true);
					}
					if (self.blockUIInput && !blockUIInput) {
						setGlassPane(new JPanel());
						getGlassPane().setVisible(false);
						Blockable.setBlocked(getJMenuBar(), false);
						setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					}
					self.blockUIInput = blockUIInput;
				}
			});
		} catch (Exception e) {
			mf.handleException(e);
		}
	}

	public void addPage() {
		insertPagesAt(numPages());
		tagsF.requestFocusInWindow();
	}

	void insertPagesAt(int atIndex) {
		JFileChooser c = new JFileChooser(mf.lastDirectory);
		c.setDialogTitle("Choose one or several pages to " +
				(atIndex == numPages() ? "add" : "insert"));
		c.setAcceptAllFileFilterUsed(false);
		c.setFileFilter(new PageFileFilter());
		c.setMultiSelectionEnabled(true);
		ImportFileAccessory ifa = new ImportFileAccessory();
		c.setAccessory(ifa);
		if (c.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			mf.lastDirectory = c.getCurrentDirectory();
			File[] fs = c.getSelectedFiles();
			if (fs != null && fs.length > 0) {
				insertPages(fs, ifa.retainedOriginal.isSelected(), ifa.deleteAfterAdding.isSelected(),
						atIndex);
			}
		}
	}

	void insertPages(final File[] pageFiles, final boolean retainOriginals,
			final boolean deleteAfterAdding, final int atIndex)
	{
		final int numPages = pageFiles.length;
		saveTags();
		new Thread("Adding page(s)") { @Override public void run() {
			mf.setUIBusy(true);
			mf.pm.showProgressBar("Adding pages", "", pageFiles.length * 2);
			ArrayList<DataSrc> fulltexts = new ArrayList<DataSrc>();
			try {
				List<Change> cs = new ArrayList<Change>();
				// Shift later pages out of the way.
				mf.pm.progress("Renumbering pages...", -1);
				int shiftIndex = numPages() - 1;
				while (shiftIndex >= atIndex) {
					cs.add(PageChange.move(string(shiftIndex), string(shiftIndex + numPages)));
					if (d.hasPage(PREVIEW_PREFIX + string(shiftIndex))) {
						cs.add(PageChange.move(PREVIEW_PREFIX + string(shiftIndex),
								PREVIEW_PREFIX + string(shiftIndex + numPages)));
					}
					if (d.hasPage(FULLTEXT_PREFIX + string(shiftIndex))) {
						cs.add(PageChange.move(FULLTEXT_PREFIX + string(shiftIndex),
								FULLTEXT_PREFIX + string(shiftIndex + numPages)));
					}
					if (d.has(HARDCOPY_NUMBER_PREFIX + string(shiftIndex))) {
						cs.add(DataChange.move(HARDCOPY_NUMBER_PREFIX + string(shiftIndex),
								HARDCOPY_NUMBER_PREFIX + string(shiftIndex + numPages)));
					}
					shiftIndex--;
				}
				int loop = 0;
				for (File f : pageFiles) {
					try {
						// Process page
						mf.pm.progress("Creating preview image of " + f.getName(), loop * 2);
						BufferedImage preview = PreviewImager.getPreviewImage(f);
						File tmp = File.createTempFile("preview", f.getName() + ".jpg");
						ImageIO.write(preview, "jpg", tmp);
						final int myIndex = atIndex + loop;
						cs.add(PageChange.put(string(myIndex), new FileSrc(f)));
						cs.add(PageChange.put(PREVIEW_PREFIX + string(myIndex), new FileSrc(tmp)));
						mf.pm.progress("Extracting full text of " + f.getName(), loop * 2 + 1);
						DataSrc ft = FullTextExtractor.getFullText(f);
						cs.add(PageChange.put(FULLTEXT_PREFIX + string(myIndex), ft));
						fulltexts.add(ft);
						if (myIndex == 0) {
							String cprof1 = ColorProfiler.profile1(preview);
							String cprof2 = ColorProfiler.profile2(preview);
							cs.add(DataChange.put(COLOR_PROFILE_1, cprof1));
							cs.add(DataChange.put(COLOR_PROFILE_2, cprof2));
							mf.tagsChanged = true;
						}
						if (retainOriginals) {
							int nextRetN = mf.store.getNextRetainedPageNumber();
							mf.store.setNextRetainedPageNumber(nextRetN + 1);
							cs.add(DataChange.put(HARDCOPY_NUMBER_PREFIX + myIndex,
									string(nextRetN)));
						}
					} catch (Exception e) {
						mf.pm.handleException(new Exception("Could not process " + f.getName() +
								" as a page.", e), null);
						return;
					}
					loop++;
				}
				mf.pm.progress("Committing data to store", -1);
				d.change(cs);
				if (mf.store.getFullTextIndex() != null) {
					mf.pm.progress("Adding pages to full text index", -1);
					for (DataSrc ft : fulltexts) { mf.store.getFullTextIndex().pageAdded(ft, d); }
				}
				if (deleteAfterAdding) {
					mf.pm.progress("Deleting originals", -1);
					for (File f : pageFiles) {
						try { f.delete(); } catch (Exception e) { /* so what */ }
					}
				}
				viewer.setPage(Math.min(atIndex, numPages() - 1));
				suggestedTagsList.update();
			} catch (Exception e) {
				mf.pm.handleException(new Exception("Could not add page(s).", e), null);
			} finally {
				mf.pm.hideProgressBar();
				mf.setUIBusy(false);
			}
		}}.start();
	}

	void moveCurrentPage() {
		final int currentIndex = viewer.getPage();
		final int numPages = numPages();
		int newIndex = currentIndex;
		String newPageNS = mf.pm.askQuestion("New location", "Where do you want to move this page?",
				string(newIndex + 1));
		try {
			newIndex = integer(newPageNS) - 1;
		} catch (Exception e) { /* meh */ }
		if (newIndex < 0) { newIndex = 0; }
		if (newIndex > numPages - 1) { newIndex = numPages - 1; }
		if (newIndex != currentIndex) {
			try {
				ArrayList<Change> cs = new ArrayList<Change>();
				cs.addAll(pageMoveChanges(
						string(currentIndex),
						string(currentIndex),
						TMP_MOVE_PAGE_INDEX));
				if (newIndex < currentIndex) {
					for (int i = currentIndex - 1; i >= newIndex; i--) {
						cs.addAll(pageMoveChanges(
							/* originalFrom */ string(i),
							/* from */         string(i),
							/* to */           string(i + 1)
						));
					}
				} else {
					// newIndex > currentIndex
					// Shift other pages to the left to make space for the page we move.
					for (int i = currentIndex + 1; i <= newIndex; i++) {
						cs.addAll(pageMoveChanges(
							/* originalFrom */ string(i),
							/* from */         string(i),
							/* to */           string(i - 1)
						));
					}
				}
				cs.addAll(pageMoveChanges(
						string(currentIndex),
						TMP_MOVE_PAGE_INDEX,
						string(newIndex)));
				d.change(cs);
				viewer.setPage(newIndex);
			} catch (FatalStorageException e) {
				mf.handleException(e);
			}
		}
	}

	List<Change> pageMoveChanges(String originalFrom, String from, String to) throws FatalStorageException {
		ArrayList<Change> cs = new ArrayList<Change>();
		cs.add(PageChange.move(from, to));
		if (d.hasPage(PREVIEW_PREFIX + originalFrom)) {
			cs.add(PageChange.move(PREVIEW_PREFIX + from,
					PREVIEW_PREFIX + to));
		}
		if (d.hasPage(FULLTEXT_PREFIX + originalFrom)) {
			cs.add(PageChange.move(FULLTEXT_PREFIX + from,
					FULLTEXT_PREFIX + to));
		}
		if (d.has(HARDCOPY_NUMBER_PREFIX + originalFrom)) {
			cs.add(DataChange.move(HARDCOPY_NUMBER_PREFIX + from,
					HARDCOPY_NUMBER_PREFIX + to));
		}
		return cs;
	}

	void deleteCurrentPage() {
		try {
			if (!deletedPageMode && viewer.validPage()) {
				final int pageNum = viewer.getPage();
				final int delPageNum = numPagesFor(DELETED_PREFIX);
				final int numPages = numPagesFor(NOT_DELETED_PREFIX);
				ArrayList<Change> cs = new ArrayList<Change>();

				// Move the page to be deleted.
				cs.addAll(pageMoveChanges(
						/* from */        string(pageNum),
						/* originalFrom */string(pageNum),
						/* to */          DELETED_PREFIX + delPageNum));

				// Shift any pages beyond this one to cover it up.
				for (int i = pageNum + 1; i < numPages; i++) {
					cs.addAll(pageMoveChanges(
						/* from */        string(i),
						/* originalFrom */string(i),
						/* to */          string(i - 1)));
				}
				d.change(cs);
				int gotoPageNum = pageNum - 1;
				if (gotoPageNum == -1 && numPages > 1) { gotoPageNum = 0; }
				viewer.setPage(gotoPageNum);
			}
		} catch (Exception e) {
			mf.handleException(new FatalStorageException("Could not delete page.", e));
		}
	}

	void undeleteCurrentPage() {
		try {
			if (deletedPageMode && viewer.validPage()) {
				final int pageNum = viewer.getPage();
				final int unDelPageNum = numPagesFor(NOT_DELETED_PREFIX);
				final int numDelPages = numPagesFor(DELETED_PREFIX);
				ArrayList<Change> cs = new ArrayList<Change>();

				// Move the page to be undeleted.
				cs.addAll(pageMoveChanges(
						/* from */        DELETED_PREFIX + pageNum,
						/* originalFrom */DELETED_PREFIX + pageNum,
						/* to */          string(unDelPageNum)));

				// Shift any pages beyond this one to cover it up.
				for (int i = pageNum + 1; i < numDelPages; i++) {
					cs.addAll(pageMoveChanges(
						/* from */        DELETED_PREFIX + i,
						/* originalFrom */DELETED_PREFIX + i,
						/* to */          DELETED_PREFIX + (i - 1)));
				}
				d.change(cs);
				int gotoPageNum = pageNum - 1;
				if (gotoPageNum == -1 && numDelPages > 1) { gotoPageNum = 0; }
				viewer.setPage(gotoPageNum);
			}
		} catch (Exception e) {
			mf.handleException(new FatalStorageException("Could not undelete page.", e));
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

	String pagePrefix() { return deletedPageMode ? DELETED_PREFIX : NOT_DELETED_PREFIX; }

	int numPages() { return numPagesFor(pagePrefix()); }

	int numPagesFor(String prefix) {
		try {
			int maxIndex = -1;
			for (String pKey : d.pageKeys()) {
				if (!pKey.startsWith(prefix)) { continue; }
				try {
					maxIndex = Math.max(maxIndex, integer(pKey.substring(prefix.length())));
				} catch (Exception e) {}
			}
			return maxIndex + 1;
		} catch (FatalStorageException e) {
			mf.handleException(e); return -1;
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
		}});
	}
}
