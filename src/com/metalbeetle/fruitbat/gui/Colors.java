package com.metalbeetle.fruitbat.gui;

import java.awt.Color;

/** Highlighting color constants. */
public final class Colors {
	private Colors() {}

	public static final Color TAG = new Color(0, 63, 191);
	public static final String TAG_HTML = toHTMLColor(TAG);

	public static final Color UNKNOWN_TAG = new Color(127, 127, 63);
	public static final String UNKNOWN_TAG_HTML = toHTMLColor(UNKNOWN_TAG);

	public static final Color MATCHED_TAG = new Color(0, 191, 0);
	public static final String MATCHED_TAG_HTML = toHTMLColor(MATCHED_TAG);


	static String toHTMLColor(Color c) {
		return "#" + toHex(c.getRed()) + toHex(c.getGreen()) + toHex(c.getBlue());
	}

	static String toHex(int channel) {
		String hex = Integer.toHexString(channel);
		return hex.length() == 1 ? "0" + hex : hex;
	}
}
