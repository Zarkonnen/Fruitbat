package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.Fruitbat;
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
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.filechooser.FileFilter;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

class DocumentFrame extends JFrame {
	static final List<String> ACCEPTED_EXTENSIONS = l(".jpg", ".tiff", ".tif", ".bmp", ".png");
	static final String PREVIEW_PREFIX = "p";
	static final String COLOR_PROFILE_1 = Fruitbat.HIDDEN_KEY_PREFIX + "cprof1";
	static final String COLOR_PROFILE_2 = Fruitbat.HIDDEN_KEY_PREFIX + "cprof2";
	static final String HARDCOPY_NUMBER_PREFIX = Fruitbat.HIDDEN_KEY_PREFIX + "ret";

	final Document d;
	final MainFrame mf;
	final DocumentMenuBar menuBar;
	InputTagCompleteMenu completeMenu;
	boolean isBeingDeleted = false;

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
		super("Fruitbat Document");
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
					tagsF.setDocument(new TagColorizingDocument(tagsF));
					tagsF.getDocument().addDocumentListener(new DocumentListener() {
						public void insertUpdate(DocumentEvent e) {
							allTagsList.update();
							suggestedTagsList.update();
						}
						public void removeUpdate(DocumentEvent e) {
							allTagsList.update();
							suggestedTagsList.update();
						}
						public void changedUpdate(DocumentEvent e) {
							allTagsList.update();
							suggestedTagsList.update();
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
						tagsF.requestFocusInWindow();
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
			public void windowClosing(WindowEvent e) {
				close();
			}

			@Override
			public void windowIconified(WindowEvent e) {
				saveTags();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				saveTags();
			}
		});

		updateTags();
		mf.app.shortcutOverlay.attachTo(this);
		pack();
		setSize(800, 800);
	}

	void close() {
		saveTags();
		mf.openDocManager.close(d);
	}

	void delete() {
		isBeingDeleted = true;
		try {
			mf.store.delete(d);
		} catch (FatalStorageException e) {
			mf.handleException(e);
		}
		dispose();
	}

	/** Show/hide a menu for completing a half-started tag. */
	void switchCompleteMenu() {
		if (completeMenu != null) {
			completeMenu.setVisible(false);
			completeMenu = null;
			return;
		}
		completeMenu = new InputTagCompleteMenu(this);
		if (completeMenu.getSubElements().length > 0) {
			completeMenu.show(tagsF, 0, tagsF.getHeight());
		} else {
			completeMenu = null;
		}
	}

	void saveTags() {
		try {
			if (!isBeingDeleted && !mf.isEmergencyShutdown) {
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
			mf.tagsChanged = true;
		} catch (FatalStorageException e) {
			mf.handleException(e);
		}
	}

	void updateTags() {
		try {
			if (d.keys().size() == 0) {
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

	void addPage() {
		JFileChooser c = new JFileChooser(mf.lastDirectory);
		c.setDialogTitle("Choose a page to add");
		c.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) { return true; }
				for (String ext : ACCEPTED_EXTENSIONS) {
					if (f.getName().endsWith(ext)) { return true; }
				}
				return false;
			}

			@Override
			public String getDescription() {
				StringBuilder sb = new StringBuilder();
				for (String ext : ACCEPTED_EXTENSIONS) {
					sb.append(ext);
					sb.append(" ");
				}
				sb.append("files");
				return sb.toString();
			}
		});
		ImportFileAccessory ifa = new ImportFileAccessory();
		c.setAccessory(ifa);
		if (c.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			mf.lastDirectory = c.getCurrentDirectory();
			File f = c.getSelectedFile();
			if (f != null && f.exists()) {
				saveTags();
				try {
					BufferedImage preview = PreviewImager.getPreviewImage(f);
					File tmp = File.createTempFile("preview", f.getName() + ".jpg");
					ImageIO.write(preview, "jpg", tmp);
					int nextIndex = numPages();
					List<Change> cs = new ArrayList<Change>();
					cs.add(PageChange.put(string(nextIndex), f));
					cs.add(PageChange.put(PREVIEW_PREFIX + string(nextIndex), tmp));
					if (nextIndex == 0) {
						String cprof1 = ColorProfiler.profile1(preview);
						String cprof2 = ColorProfiler.profile2(preview);
						cs.add(DataChange.put(COLOR_PROFILE_1, cprof1));
						cs.add(DataChange.put(COLOR_PROFILE_2, cprof2));
						mf.tagsChanged = true;
					}
					if (ifa.retainedOriginal.isSelected()) {
						int nextRetN = mf.store.getNextRetainedPageNumber();
						mf.store.setNextRetainedPageNumber(nextRetN + 1);
						cs.add(DataChange.put(HARDCOPY_NUMBER_PREFIX + nextIndex,
								string(nextRetN)));
					}
					d.change(cs);
					viewer.setPage(nextIndex);
					suggestedTagsList.update();
					if (ifa.deleteAfterAdding.isSelected()) {
						f.delete();
					}
				} catch (Exception e) {
					e.printStackTrace(); // TODO
				}
			}
		}
	}

	static class ImportFileAccessory extends Box {
		final JCheckBox deleteAfterAdding;
		final JCheckBox retainedOriginal;

		public ImportFileAccessory() {
			super(BoxLayout.Y_AXIS);
			add(deleteAfterAdding = new JCheckBox("Delete original"));
				deleteAfterAdding.setSelected(true);
				deleteAfterAdding.setToolTipText("Deletes the original file after adding it to " +
						"the document as a page.");
			add(retainedOriginal = new JCheckBox("Assign hardcopy number"));
				retainedOriginal.setToolTipText("If you are keeping the original paper copy of " +
						"this page, this gives it an unique number you can file it under.");
		}
	}

	int numPages() {
		try {
			int maxIndex = -1;
			for (String pKey : d.pageKeys()) {
				try {
					maxIndex = Math.max(maxIndex, integer(pKey));
				} catch (Exception e) {}
			}
			return maxIndex + 1;
		} catch (FatalStorageException e) {
			mf.handleException(e); return -1;
		}
	}

	public void writePrefs(Preferences p) throws BackingStoreException, FatalStorageException {
		if (isVisible()) {
			p.putInt("x", getX());
			p.putInt("y", getY());
			p.putInt("width", getWidth());
			p.putInt("height", getHeight());
			p.putBoolean("focused", isFocused());
			p.putInt("pageNum", viewer.getPage());
			p.putInt("suggestedTagsScrollX", suggestedTagsListSP.getViewport().getViewPosition().x);
			p.putInt("suggestedTagsScrollY", suggestedTagsListSP.getViewport().getViewPosition().y);
			p.putInt("allTagsScrollX", allTagsListSP.getViewport().getViewPosition().x);
			p.putInt("allTagsScrollY", allTagsListSP.getViewport().getViewPosition().y);
		}
	}

	public void readPrefs(final Preferences p) throws BackingStoreException, FatalStorageException {
		setLocation(p.getInt("x", getX()), p.getInt("y", getY()));
		setSize(p.getInt("width", getWidth()), p.getInt("height", getHeight()));
		viewer.setPage(p.getInt("pageNum", 0));
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			suggestedTagsListSP.getViewport().setViewPosition(
					new Point(p.getInt("suggestedTagsScrollX", 0), p.getInt("suggestedTagsScrollY", 0)));
			allTagsListSP.getViewport().setViewPosition(
					new Point(p.getInt("allTagsScrollX", 0), p.getInt("allTagsScrollY", 0)));
		}});
	}
}
