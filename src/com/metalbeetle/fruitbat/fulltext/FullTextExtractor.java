package com.metalbeetle.fruitbat.fulltext;

import com.metalbeetle.fruitbat.io.ByteArraySrc;
import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.io.StringSrc;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/** Attempts to extract the text in a given file. */
public final class FullTextExtractor {
	private FullTextExtractor() {}

	public static DataSrc getFullText(File f) {
		try {
			if (f.getName().toLowerCase().endsWith(".pdf")) {
				PDDocument pd = null;
				try {
					pd = PDDocument.load(f);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					new PDFTextStripper().writeText(pd, new OutputStreamWriter(baos));
					return new ByteArraySrc(baos.toByteArray(), "full text of " + f.getName() +
							".txt");
				} finally {
					try { pd.close(); } catch (Exception e) {}
				}
			} else {
				ProcessBuilder pb = new ProcessBuilder("ocroscript", "recognize", f.getAbsolutePath());
				Process ocroProcess = pb.start();
				OutputEater outE = new OutputEater(ocroProcess.getInputStream());
				OutputEater errE = new OutputEater(ocroProcess.getErrorStream());
				outE.start();
				errE.start();
				int returnCode = ocroProcess.waitFor();
				if (returnCode == 0) {
					return new StringSrc(HTMLToText.toText(outE.get()),
							"full text of " + f.getName() + ".txt");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new StringSrc("", "full text of " + f.getName() + ".txt");
	}
}
