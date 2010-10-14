package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.util.ColorProfiler;
import com.metalbeetle.fruitbat.fulltext.FullTextExtractor;
import com.metalbeetle.fruitbat.io.FileSrc;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import com.metalbeetle.fruitbat.util.PreviewImager;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.Change;
import com.metalbeetle.fruitbat.storage.PageChange;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.gui.DocumentFrame.*;

class InsertPagesWorker extends SwingWorker<Void, Void> {
	final DocumentFrame df;
	final MainFrame mf;
	final File[] pageFiles;
	final boolean retainOriginals;
	final boolean deleteAfterAdding;
	final int atIndex;

	public InsertPagesWorker(DocumentFrame df, File[] pageFiles, boolean retainOriginals,
			boolean deleteAfterAdding, int atIndex)
	{
		this.df = df;
		this.mf = df.mf;
		this.pageFiles = pageFiles;
		this.retainOriginals = retainOriginals;
		this.deleteAfterAdding = deleteAfterAdding;
		this.atIndex = atIndex;
	}

	@Override
	protected Void doInBackground() throws Exception {
		final int numPages = pageFiles.length;
		mf.pm.showProgressBar("Adding pages", "", pageFiles.length * 2);
		ArrayList<DataSrc> fulltexts = new ArrayList<DataSrc>();
		try {
			List<Change> cs = new ArrayList<Change>();
			// Shift later pages out of the way.
			mf.pm.progress("Renumbering pages...", -1);
			int shiftIndex = df.numPages() - 1;
			while (shiftIndex >= atIndex) {
				cs.add(PageChange.move(string(shiftIndex), string(shiftIndex + numPages)));
				if (df.d.hasPage(PREVIEW_PREFIX + string(shiftIndex))) {
					cs.add(PageChange.move(PREVIEW_PREFIX + string(shiftIndex),
							PREVIEW_PREFIX + string(shiftIndex + numPages)));
				}
				if (df.d.hasPage(FULLTEXT_PREFIX + string(shiftIndex))) {
					cs.add(PageChange.move(FULLTEXT_PREFIX + string(shiftIndex),
							FULLTEXT_PREFIX + string(shiftIndex + numPages)));
				}
				if (df.d.has(HARDCOPY_NUMBER_PREFIX + string(shiftIndex))) {
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
					return null;
				}
				loop++;
			}
			mf.pm.progress("Committing data to store", -1);
			df.d.change(cs);
			if (mf.store.getFullTextIndex() != null) {
				mf.pm.progress("Adding pages to full text index", -1);
				for (DataSrc ft : fulltexts) { mf.store.getFullTextIndex().pageAdded(ft, df.d); }
			}
			if (deleteAfterAdding) {
				mf.pm.progress("Deleting originals", -1);
				for (File f : pageFiles) {
					try { f.delete(); } catch (Exception e) { /* so what */ }
				}
			}
			df.viewer.setPage(Math.min(atIndex, df.numPages() - 1));
			df.suggestedTagsList.update();
		} catch (Exception e) {
			mf.pm.handleException(new Exception("Could not add page(s).", e), null);
		}
		return null;
	}

	@Override
	protected void done() {
		df.mf.pm.hideProgressBar();
	}
}
