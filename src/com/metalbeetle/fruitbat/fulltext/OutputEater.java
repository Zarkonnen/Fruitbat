package com.metalbeetle.fruitbat.fulltext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class OutputEater extends Thread {
	private final BufferedReader r;
	private final StringBuilder sb = new StringBuilder();

	OutputEater(InputStream s) {
		r = new BufferedReader(new InputStreamReader(s));
	}

	public String get() throws InterruptedException {
		join();
		return sb.toString();
	}

	@Override
	public void run() {
		try {
			char[] buf = new char[1024];
			int read = -1;
			while ((read = r.read(buf)) != -1) {
				sb.append(buf, 0, read);
			}
		} catch (IOException e) {
			e.printStackTrace();
			sb.delete(0, sb.length());
		}
	}
}
