package com.metalbeetle.fruitbat.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ColorProfiler {
	public static String profile1(BufferedImage imgA) {
		String p = profile(imgA, ColorProfilerSettings.DEFAULT_1);
		return p.length() != 0 ? p : profile(imgA, ColorProfilerSettings.SENSITIVE_1);
	}

	public static String profile2(BufferedImage imgA) {
		String p = profile(imgA, ColorProfilerSettings.DEFAULT_2);
		return p.length() != 0 ? p : profile(imgA, ColorProfilerSettings.SENSITIVE_2);
	}

	public static String profile(BufferedImage imgA, ColorProfilerSettings s) {
		BufferedImage img = new BufferedImage(s.imgW, s.imgW * imgA.getHeight() / imgA.getWidth(),
				BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.drawImage(imgA, 0, 0, img.getWidth(), img.getHeight(), null);
		g.dispose();
		HashMap<Color, Integer> prof = new HashMap<Color, Integer>();
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				Color col = new Color(img.getRGB(x, y));
				Color dbcol = new Color(
						(int) Math.max(0, col.getRed() - s.brown.getRed() * s.deBrownIntensity),
						(int) Math.max(0, col.getGreen() - s.brown.getGreen() * s.deBrownIntensity),
						(int) Math.max(0, col.getBlue() - s.brown.getBlue() * s.deBrownIntensity)
				);
				int sat = 0;
				sat += Math.abs(dbcol.getRed() - dbcol.getGreen());
				sat += Math.abs(dbcol.getRed() - dbcol.getBlue());
				sat += Math.abs(dbcol.getGreen() - dbcol.getBlue());
				if (sat < s.minSaturation) { continue; }
				Color col2 = new Color(
						col.getRed()   / s.compress,
						col.getGreen() / s.compress,
						col.getBlue()  / s.compress);
				if (prof.containsKey(col2)) {
					prof.put(col2, prof.get(col2) + 1);
				} else {
					prof.put(col2, 1);
				}
			}
		}
		List<Map.Entry<Color, Integer>> cs = new ArrayList<Map.Entry<Color, Integer>>(prof.entrySet());
		Collections.sort(cs, new Comparator<Map.Entry<Color, Integer>>() {
			public int compare(Entry<Color, Integer> o1, Entry<Color, Integer> o2) {
				return o2.getValue() - o1.getValue();
			}
		});

		String profString = "";

		for (int i = 0; i < cs.size() && i < 3; i++) {
			if (cs.get(i).getValue() < cs.get(0).getValue() * s.inclusionFraction) {
				break;
			}
			profString +=
					cs.get(i).getKey().getRed() == 0
					? "00"
					: Integer.toHexString(cs.get(i).getKey().getRed() * s.compress);
			profString +=
					cs.get(i).getKey().getGreen() == 0
					? "00"
					: Integer.toHexString(cs.get(i).getKey().getGreen() * s.compress);
			profString +=
					cs.get(i).getKey().getBlue() == 0
					? "00"
					: Integer.toHexString(cs.get(i).getKey().getBlue() * s.compress);
		}
		return profString;
	}
}
