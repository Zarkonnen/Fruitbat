package com.metalbeetle.fruitbat.gui;

import java.awt.Color;

/** Highlighting color constants. */
public final class Colors {
	private Colors() {}

	public static final Color TAG = new Color(0, 63, 191);
	public static final String TAG_HTML = toHTMLColor(TAG);

	public static final Color UNKNOWN_TAG = new Color(91, 91, 91);
	public static final String UNKNOWN_TAG_HTML = toHTMLColor(UNKNOWN_TAG);

	public static final Color MATCHED_TAG = new Color(0, 191, 0);
	public static final String MATCHED_TAG_HTML = toHTMLColor(MATCHED_TAG);

	public static final Color UNUSED_TAG = new Color(0, 15, 47);
	public static final String UNUSED_TAG_HTML = toHTMLColor(UNUSED_TAG);

	public static final Color IGNORED_TAG = new Color(191, 127, 63);
	public static final String IGNORED_TAG_HTML = toHTMLColor(IGNORED_TAG);

	public static final Color VALUE = new Color(91, 0, 91);
	public static final String VALUE_HTML = toHTMLColor(VALUE);

	public static final Color TAG_BG = new Color(230, 230, 255);
	public static final String TAG_BG_HTML = toHTMLColor(TAG_BG);

	public static final Color HARDCOPY_NUM_BG = new Color(240, 240, 20);

	public static final Color FULL_TEXT = Color.BLACK;
	public static final String FULL_TEXT_HTML = toHTMLColor(FULL_TEXT);

	static String toHTMLColor(Color c) {
		return "#" + toHex(c.getRed()) + toHex(c.getGreen()) + toHex(c.getBlue());
	}

	static String toHex(int channel) {
		String hex = Integer.toHexString(channel);
		return hex.length() == 1 ? "0" + hex : hex;
	}
}
