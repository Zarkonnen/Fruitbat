package com.metalbeetle.fruitbat.fulltext;

import java.io.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

public class HTMLToText extends HTMLEditorKit.ParserCallback {
	StringBuffer s;

	void parse(Reader in) throws IOException {
		s = new StringBuffer();
		ParserDelegator delegator = new ParserDelegator();
		// the third parameter is TRUE to ignore charset directive
		delegator.parse(in, this, Boolean.TRUE);
	}

	@Override
	public void handleText(char[] text, int pos) {
		s.append(text);
	}

	String getText() {
		return s.toString();
	}

	public static String toText(String in) throws IOException {
		HTMLToText me = new HTMLToText();
		me.parse(new StringReader(in));
		return me.getText();
	}
}
