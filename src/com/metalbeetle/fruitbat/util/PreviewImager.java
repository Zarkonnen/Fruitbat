package com.metalbeetle.fruitbat.util;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

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
