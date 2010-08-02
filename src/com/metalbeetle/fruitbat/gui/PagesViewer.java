package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.storage.DataChange;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
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
			setPage(Integer.valueOf(pageField.getText()), /*setPageFieldText*/ false);
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
				df.d.getPage(string(pv.pageIndex)).getPath()
			});
		} catch (Exception ex) {}
	}

	void assignHardcopyNumber() {
		try {
			int nextRetN = df.mf.store.getNextRetainedPageNumber();
			df.mf.store.setNextRetainedPageNumber(nextRetN + 1);
			df.d.change(l(DataChange.put(DocumentFrame.HARDCOPY_NUMBER_PREFIX + pv.pageIndex,
					string(nextRetN))));
			updateMenuEnabledStates();
			pv.repaint();
		} catch (FatalStorageException e) {
			df.mf.handleException(e);
		}
	}

	void removeHardcopyNumber() {
		try {
			df.d.change(l(DataChange.remove(DocumentFrame.HARDCOPY_NUMBER_PREFIX + pv.pageIndex)));
			updateMenuEnabledStates();
			pv.repaint();
		} catch (FatalStorageException e) {
			df.mf.handleException(e);
		}
	}

	void setPage(int pageIndex) { setPage(pageIndex, true); }

	void setPage(int pageIndex, boolean setPageFieldText) {
		int numPages = df.numPages();
		if (pageIndex < 0) { pageIndex = 0; }
		if (pageIndex >= numPages) { pageIndex = numPages - 1; }
		pv.pageIndex = pageIndex;
		updateMenuEnabledStates();
		if (setPageFieldText) {
			pageField.setText(string(pv.pageIndex + 1));
		}
		ofPagesL.setText(" / " + numPages);
		pv.repaint();
	}

	void updateMenuEnabledStates() {
		prevButton.setEnabled(hasPrevPage());
		nextButton.setEnabled(hasNextPage());
		openButton.setEnabled(validPage());
		df.menuBar.prevPageMI.setEnabled(hasPrevPage());
		df.menuBar.nextPageMI.setEnabled(hasNextPage());
		df.menuBar.openPageMI.setEnabled(validPage());
		df.menuBar.gotoPageMI.setEnabled(df.numPages() > 0);
		df.menuBar.assignHCNMI.setVisible(!hasHCN());
		df.menuBar.removeHCNMI.setVisible(hasHCN());
		df.menuBar.assignHCNMI.setEnabled(validPage());
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
			return validPage() && df.d.has(DocumentFrame.HARDCOPY_NUMBER_PREFIX + pv.pageIndex);
		} catch (FatalStorageException e) {
			df.mf.handleException(e); return false;
		}
	}

	static class PageViewer extends JPanel {
		final PagesViewer pv;
		File imgFile;
		BufferedImage docImg;
		int pageIndex = 0;
		String hardcopyNum;

		PageViewer(PagesViewer pv) {
			this.pv = pv;
		}

		@Override
		public void paint(Graphics g) {
			try {
				URI pageURI = pv.df.d.getPage(DocumentFrame.PREVIEW_PREFIX + pageIndex);
				File f = new File(pageURI.getPath());
				if (!f.equals(imgFile)) {
					imgFile = f;
					docImg = ImageIO.read(f);
				}
				g.drawImage(docImg, 0, 0, getWidth(),
						docImg.getHeight() * getWidth() / docImg.getWidth(), this);
				hardcopyNum =
							pv.df.d.has(DocumentFrame.HARDCOPY_NUMBER_PREFIX + pageIndex)
							? pv.df.d.get(DocumentFrame.HARDCOPY_NUMBER_PREFIX + pageIndex)
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
			} catch (Exception e) {
				g.setColor(Color.GRAY);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		}
	}
}
