package com.metalbeetle.fruitbat.storage;

import com.metalbeetle.fruitbat.Fruitbat;
import com.metalbeetle.fruitbat.ProgressMonitor;
import com.metalbeetle.fruitbat.fulltext.FullTextExtractor;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.io.FileSrc;
import com.metalbeetle.fruitbat.util.ColorProfiler;
import com.metalbeetle.fruitbat.util.PreviewImager;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

/**
 * Extra functionality for documents.
 */
public class DocumentTools {
	static final List<String> ACCEPTED_EXTENSIONS = l(".jpg", ".tiff", ".tif", ".bmp", ".png",
			".pdf");
	public static final String PREVIEW_PREFIX = "p";
	public static final String COLOR_PROFILE_1 = Fruitbat.HIDDEN_KEY_PREFIX + "cprof1";
	public static final String COLOR_PROFILE_2 = Fruitbat.HIDDEN_KEY_PREFIX + "cprof2";
	public static final String HARDCOPY_NUMBER_PREFIX = Fruitbat.HIDDEN_KEY_PREFIX + "ret";
	public static final String FULLTEXT_PREFIX = Fruitbat.HIDDEN_KEY_PREFIX + "ft";
	public static final String TMP_MOVE_PAGE_INDEX = "tmp";
	public static final String DELETED_PREFIX = "d";
	public static final String NOT_DELETED_PREFIX = "";
	
	public static int numPagesFor(Document d, String prefix) throws FatalStorageException {
		int maxIndex = -1;
		for (String pKey : d.pageKeys()) {
			if (!pKey.startsWith(prefix)) { continue; }
			try {
				maxIndex = Math.max(maxIndex, integer(pKey.substring(prefix.length())));
			} catch (Exception e) {}
		}
		return maxIndex + 1;
	}

	public static boolean insertPages(
			Document d,
			Store store,
			ProgressMonitor pm,
			final File[] pageFiles,
			final boolean retainOriginals,
			final boolean deleteAfterAdding,
			final int atIndex,
			final int numPages)
	{
		pm.newProcess("Adding pages", "", pageFiles.length * 2);
		ArrayList<DataSrc> fulltexts = new ArrayList<DataSrc>();
		try {
			List<Change> cs = new ArrayList<Change>();
			// Shift later pages out of the way.
			pm.progress("Renumbering pages...", -1);
			int shiftIndex = numPages - 1;
			while (shiftIndex >= atIndex) {
				if (d.hasPage(string(shiftIndex))) {
					cs.add(PageChange.move(string(shiftIndex), string(shiftIndex + numPages)));
				}
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
					pm.progress("Creating preview image of " + f.getName(), loop * 2);
					BufferedImage preview = PreviewImager.getPreviewImage(f);
					File tmp = File.createTempFile("preview", f.getName() + ".jpg");
					ImageIO.write(preview, "jpg", tmp);
					final int myIndex = atIndex + loop;
					cs.add(PageChange.put(string(myIndex), new FileSrc(f)));
					cs.add(PageChange.put(PREVIEW_PREFIX + string(myIndex), new FileSrc(tmp)));
					pm.progress("Extracting full text of " + f.getName(), loop * 2 + 1);
					DataSrc ft = FullTextExtractor.getFullText(f);
					cs.add(PageChange.put(FULLTEXT_PREFIX + string(myIndex), ft));
					fulltexts.add(ft);
					if (myIndex == 0) {
						String cprof1 = ColorProfiler.profile1(preview);
						String cprof2 = ColorProfiler.profile2(preview);
						cs.add(DataChange.put(COLOR_PROFILE_1, cprof1));
						cs.add(DataChange.put(COLOR_PROFILE_2, cprof2));
					}
					if (retainOriginals) {
						int nextRetN = store.getNextRetainedPageNumber();
						store.setNextRetainedPageNumber(nextRetN + 1);
						cs.add(DataChange.put(HARDCOPY_NUMBER_PREFIX + myIndex,
								string(nextRetN)));
					}
				} catch (Exception e) {
					pm.handleException(new Exception("Could not process " + f.getName() +
							" as a page.", e), null);
					return false;
				}
				loop++;
			}
			pm.progress("Committing data to store", -1);
			d.change(cs);
			if (store.getFullTextIndex() != null) {
				pm.progress("Adding pages to full text index", -1);
				for (DataSrc ft : fulltexts) { store.getFullTextIndex().pageAdded(ft, d); }
			}
			if (deleteAfterAdding) {
				pm.progress("Deleting originals", -1);
				for (File f : pageFiles) {
					try { f.delete(); } catch (Exception e) { /* so what */ }
				}
			}
		} catch (Exception e) {
			pm.handleException(new Exception("Could not add page(s).", e), null);
			return false;
		}
		return true;
	}

	public static void movePage(Document d, EnhancedStore store, ProgressMonitor pm, int currentIndex, int newIndex) throws FatalStorageException {
		ArrayList<Change> cs = new ArrayList<Change>();
		cs.addAll(pageMoveChanges(
				d,
				string(currentIndex),
				string(currentIndex),
				TMP_MOVE_PAGE_INDEX));
		if (newIndex < currentIndex) {
			for (int i = currentIndex - 1; i >= newIndex; i--) {
				cs.addAll(pageMoveChanges(
					d,
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
					d,
					/* originalFrom */ string(i),
					/* from */         string(i),
					/* to */           string(i - 1)
				));
			}
		}
		cs.addAll(pageMoveChanges(
				d,
				string(currentIndex),
				TMP_MOVE_PAGE_INDEX,
				string(newIndex)));
		d.change(cs);
	}

	static List<Change> pageMoveChanges(Document d, String originalFrom, String from, String to) throws FatalStorageException {
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

	public static void deletePage(Document d, EnhancedStore store, ProgressMonitor pm, int pageNum) throws FatalStorageException {
		try {
			final int delPageNum = numPagesFor(d, DELETED_PREFIX);
			final int numPages = numPagesFor(d, NOT_DELETED_PREFIX);
			ArrayList<Change> cs = new ArrayList<Change>();

			// Move the page to be deleted.
			cs.addAll(pageMoveChanges(
					d,
					/* from */        string(pageNum),
					/* originalFrom */string(pageNum),
					/* to */          DELETED_PREFIX + delPageNum));

			// Shift any pages beyond this one to cover it up.
			for (int i = pageNum + 1; i < numPages; i++) {
				cs.addAll(pageMoveChanges(
					d,
					/* from */        string(i),
					/* originalFrom */string(i),
					/* to */          string(i - 1)));
			}
			d.change(cs);
		} catch (Exception e) {
			throw new FatalStorageException("Could not delete page.", e);
		}
	}

	public static void undeletePage(Document d, EnhancedStore store, ProgressMonitor pm, int pageNum) throws FatalStorageException {
		try {
			final int unDelPageNum = numPagesFor(d, NOT_DELETED_PREFIX);
			final int numDelPages = numPagesFor(d, DELETED_PREFIX);
			ArrayList<Change> cs = new ArrayList<Change>();

			// Move the page to be undeleted.
			cs.addAll(pageMoveChanges(
					d,
					/* from */        DELETED_PREFIX + pageNum,
					/* originalFrom */DELETED_PREFIX + pageNum,
					/* to */          string(unDelPageNum)));

			// Shift any pages beyond this one to cover it up.
			for (int i = pageNum + 1; i < numDelPages; i++) {
				cs.addAll(pageMoveChanges(
					d,
					/* from */        DELETED_PREFIX + i,
					/* originalFrom */DELETED_PREFIX + i,
					/* to */          DELETED_PREFIX + (i - 1)));
			}
			d.change(cs);
		} catch (Exception e) {
			throw new FatalStorageException("Could not undelete page.", e);
		}
	}
}
