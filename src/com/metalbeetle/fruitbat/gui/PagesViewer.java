package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.io.FileSrc;
import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.gui.Colors.*;


class PagesViewer extends JPanel {
	final DocumentFrame df;
	boolean deletedPageMode = false;
	int otherPageListIndex = -1;

	final Box topBox;
		final JButton prevButton;
		final JLabel pageL;
		final JTextField pageField;
		final JLabel ofPagesL;
		final JButton nextButton;
		final JButton openButton;
		final JPanel viewerP;
			final PageViewer pv;

	PagesViewer(final DocumentFrame df) {
		this.df = df;
		setLayout(new BorderLayout());
		add(topBox = Box.createHorizontalBox(), BorderLayout.NORTH);
			topBox.add(pageL = new JLabel("Page "));
			topBox.add(pageField = new JTextField(/*cols*/3));
				pageField.setMaximumSize(new Dimension(40, 40));
				pageField.getDocument().addDocumentListener(new DocumentListener() {
					public void insertUpdate(DocumentEvent e) {
						gotoInputPage();
					}
					public void removeUpdate(DocumentEvent e) {
						gotoInputPage();
					}
					public void changedUpdate(DocumentEvent e) {
						gotoInputPage();
					}
				});
			topBox.add(ofPagesL = new JLabel());
			topBox.add(Box.createHorizontalStrut(10));
			topBox.add(prevButton = Buttons.getIconButton("leftarrow.png"));
				prevButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					prevPage();
					df.tagsF.requestFocusInWindow();
				}});
				prevButton.setFocusable(false);
			topBox.add(nextButton = Buttons.getIconButton("rightarrow.png"));
				nextButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					nextPage();
					df.tagsF.requestFocusInWindow();
				}});
				nextButton.setFocusable(false);
			topBox.add(openButton = new JButton("Open in editor"));
				openButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
					openPage();
					df.tagsF.requestFocusInWindow();
				}});
				openButton.setFocusable(false);
			add(viewerP = new JPanel(new BorderLayout()), BorderLayout.CENTER);
				viewerP.setBorder(new BevelBorder(BevelBorder.LOWERED));
				viewerP.add(pv = new PageViewer(this), BorderLayout.CENTER);
			setPage(1);
	}

	void gotoInputPage() {
		try {
			setPage(
					Integer.valueOf(pageField.getText()) - 1,
					/*setPageFieldText*/ false,
					/*updatedisplay*/ true);
		} catch (NumberFormatException e) {}
	}

	void nextPage() {
		setPage(pv.pageIndex + 1);
	}

	void prevPage() {
		setPage(pv.pageIndex - 1);
	}

	void openPage() {
		try {
			Runtime.getRuntime().exec(new String[] {
				"open",
				((FileSrc) df.d.getPage(df.pagePrefix() + pv.pageIndex)).f.getPath()
			});
		} catch (Exception ex) {}
	}

	void assignHardcopyNumber() {
		try {
			int nextRetN = df.mf.store.getNextRetainedPageNumber();
			df.mf.store.setNextRetainedPageNumber(nextRetN + 1);
			df.d.change(l(DataChange.put(df.pagePrefix() + DocumentFrame.HARDCOPY_NUMBER_PREFIX +
					pv.pageIndex, string(nextRetN))));
			updateDisplay();
			pv.repaint();
		} catch (FatalStorageException e) {
			df.mf.handleException(e);
		}
	}

	void removeHardcopyNumber() {
		try {
			df.d.change(l(DataChange.remove(df.pagePrefix() + DocumentFrame.HARDCOPY_NUMBER_PREFIX +
					pv.pageIndex)));
			updateDisplay();
			pv.repaint();
		} catch (FatalStorageException e) {
			df.mf.handleException(e);
		}
	}

	void setPage(int pageIndex) { setPage(pageIndex, true, true); }

	void setPage(int pageIndex, boolean setPageFieldText, boolean updateDisplay) {
		int numPages = df.numPages();
		if (pageIndex < 0) { pageIndex = 0; }
		if (pageIndex >= numPages) { pageIndex = numPages - 1; }
		pv.pageIndex = pageIndex;
		if (updateDisplay) { updateDisplay(); }
		if (setPageFieldText) {
			pageField.setText(string(pv.pageIndex + 1));
		}
		ofPagesL.setText(" / " + numPages);
		pv.repaint();
	}

	int getPage() {
		return pv.pageIndex;
	}

	void updateDisplay() {
		if (df.deletedPageMode != deletedPageMode) {
			deletedPageMode = df.deletedPageMode;
			int tmp = otherPageListIndex;
			otherPageListIndex = getPage();
			setPage(tmp, true, false);
		}
		prevButton.setEnabled(hasPrevPage());
		nextButton.setEnabled(hasNextPage());
		openButton.setEnabled(validPage());
		df.menuBar.prevPageMI.setEnabled(hasPrevPage());
		df.menuBar.nextPageMI.setEnabled(hasNextPage());
		df.menuBar.openPageMI.setEnabled(validPage());
		df.menuBar.gotoPageMI.setEnabled(df.numPages() > 0);
		df.menuBar.assignHCNMI.setVisible(!hasHCN());
		df.menuBar.removeHCNMI.setVisible(hasHCN());
		df.menuBar.assignHCNMI.setEnabled(validPage() && !df.isDeleted());
		df.menuBar.removeHCNMI.setEnabled(!df.isDeleted());
		df.menuBar.deletePageMI.setEnabled(validPage() && !deletedPageMode);
		df.menuBar.undeletePageMI.setEnabled(validPage() && deletedPageMode);
	}

	boolean hasPrevPage() {
		return pv.pageIndex > 0;
	}

	boolean hasNextPage() {
		return pv.pageIndex < df.numPages() - 1;
	}

	boolean validPage() {
		return pv.pageIndex != -1;
	}

	boolean hasHCN() {
		try {
			return validPage() && df.d.has(df.pagePrefix() + DocumentFrame.HARDCOPY_NUMBER_PREFIX +
					pv.pageIndex);
		} catch (FatalStorageException e) {
			df.mf.handleException(e); return false;
		}
	}

	static class PageViewer extends JPanel {
		final PagesViewer pv;
		DataSrc imgSrc;
		BufferedImage docImg;
		int pageIndex = 0;
		String hardcopyNum;

		PageViewer(PagesViewer pv) {
			this.pv = pv;
		}

		@Override
		public void paint(Graphics g) {
			try {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, getWidth(), getHeight());
				DataSrc src = pv.df.d.getPage(DocumentFrame.PREVIEW_PREFIX +
						pv.df.pagePrefix() + pageIndex);
				if (!src.equals(imgSrc)) {
					imgSrc = src;
					docImg = ImageIO.read(src.getInputStream());
				}
				g.drawImage(docImg, 0, 0, getWidth(),
						docImg.getHeight() * getWidth() / docImg.getWidth(), this);
				hardcopyNum =
							pv.df.d.has(DocumentFrame.HARDCOPY_NUMBER_PREFIX + pv.df.pagePrefix() + pageIndex)
							? pv.df.d.get(DocumentFrame.HARDCOPY_NUMBER_PREFIX + pv.df.pagePrefix() + pageIndex)
							: null;
				if (hardcopyNum != null) {
					hardcopyNum = "Hardcopy #" + hardcopyNum;
					g.setFont(new JLabel().getFont().deriveFont(12f));
					int w = g.getFontMetrics().stringWidth(hardcopyNum);
					g.setColor(HARDCOPY_NUM_BG);
					g.fillRect(
							getWidth() - 25 - w,
							10,
							w + 10,
							16 + 10
							);
					g.setColor(Color.BLACK);
					g.drawString(hardcopyNum, getWidth() - 20 - w, 28);
				}
				if (pv.deletedPageMode) {
					g.setColor(DELETED_TINT);
					g.fillRect(0, 0, getWidth(), getHeight());
					g.setFont(new JLabel().getFont().deriveFont(24f));
					int w = g.getFontMetrics().stringWidth("DELETED");
					g.setColor(DELETED_BG);
					g.fillRoundRect(
							getWidth() / 2 - w / 2,
							getHeight() / 4,
							w + 10,
							34,
							5,
							5
					);
					g.setColor(Color.BLACK);
					g.drawString("DELETED", getWidth() / 2 - w / 2 + 5, getHeight() / 4 + 25);
				}
			} catch (Exception e) {
				g.setColor(Color.GRAY);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		}
	}
}
