package com.metalbeetle.fruitbat.util;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class PreviewImager {
	public static final int IMG_WIDTH = 800;
	static final BufferedImage NO_PREVIEW_IMAGE;
	static {
		BufferedImage img = null;
		try {
			img = ImageIO.read(PreviewImager.class.getResourceAsStream("nopreview.png"));
		} catch (IOException e) {
			img = new BufferedImage(IMG_WIDTH, IMG_WIDTH, BufferedImage.TYPE_INT_RGB);
		}
		NO_PREVIEW_IMAGE = img;
	}

	public static BufferedImage getPreviewImage(File f) {
		try {
			if (f.getName().toLowerCase().endsWith(".pdf")) {
				PDDocument pd = null;
				try {
					pd = PDDocument.load(f);
					if (pd.getDocumentCatalog().getAllPages().size() == 0) {
						return NO_PREVIEW_IMAGE;
					}
					PDPage p = (PDPage) pd.getDocumentCatalog().getAllPages().get(0);
					float widthPt = p.findMediaBox().getWidth();
					// Arrange a resolution parameter that will hopefully result in approximately 800 px
					// width.
					int resolution = (int) (IMG_WIDTH * 72 / widthPt);
					return p.convertToImage(BufferedImage.TYPE_INT_RGB, resolution);
				} finally {
					try { pd.close(); } catch (Exception e) {}
				}
			}
			
			BufferedImage img = ImageIO.read(f);
			BufferedImage preview = new BufferedImage(IMG_WIDTH,
					IMG_WIDTH * img.getHeight() / img.getWidth(), BufferedImage.TYPE_INT_RGB);
			Graphics g = preview.getGraphics();
			g.drawImage(img, 0, 0, preview.getWidth(), preview.getHeight(), null);
			g.dispose();
			preview.flush();
			return preview;
		} catch (Exception e) {
			return NO_PREVIEW_IMAGE;
		}
	}
}
