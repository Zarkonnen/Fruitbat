package com.metalbeetle.fruitbat.util;

import java.awt.Color;

public class ColorProfilerSettings {
	public static final Color DEFAULT_BROWN = new Color(192, 168, 20);

	public static final ColorProfilerSettings DEFAULT_1 = new ColorProfilerSettings(
			/*compress*/          64,
			/*minSaturation*/    180,
			/*deBrownIntensity*/   0.08,
			/*inclusionFraction*/  0.6,
			/*imgW*/              64,
			/*minCount*/           0);

	public static final ColorProfilerSettings SENSITIVE_1 = new ColorProfilerSettings(
			/*compress*/          64,
			/*minSaturation*/    160,
			/*deBrownIntensity*/   0.05,
			/*inclusionFraction*/  0.75,
			/*imgW*/              100,
			/*minCount*/           0);

	public static final ColorProfilerSettings DEFAULT_2 = new ColorProfilerSettings(
			/*compress*/          48,
			/*minSaturation*/    170,
			/*deBrownIntensity*/   0.07,
			/*inclusionFraction*/  0.55,
			/*imgW*/              92,
			/*minCount*/           0);

	public static final ColorProfilerSettings SENSITIVE_2 = new ColorProfilerSettings(
			/*compress*/          48,
			/*minSaturation*/    140,
			/*deBrownIntensity*/   0.045,
			/*inclusionFraction*/  0.7,
			/*imgW*/              150,
			/*minCount*/           0);

	public final int compress;
	public final int minSaturation;
	public final double deBrownIntensity;
	public final double inclusionFraction;
	public final int imgW;
	public final Color brown;
	public final int minCount;

	public ColorProfilerSettings(int compress, int minSaturation, double deBrownIntensity,
			double inclusionFraction, int imgW, int minCount)
	{
		this(compress, minSaturation, deBrownIntensity, inclusionFraction, imgW, DEFAULT_BROWN,
				minCount);
	}

	public ColorProfilerSettings(int compress, int minSaturation, double deBrownIntensity,
			double inclusionFraction, int imgW, Color brown, int minCount)
	{
		this.compress = compress;
		this.minSaturation = minSaturation;
		this.deBrownIntensity = deBrownIntensity;
		this.inclusionFraction = inclusionFraction;
		this.imgW = imgW;
		this.brown = brown;
		this.minCount = minCount;
	}
}
