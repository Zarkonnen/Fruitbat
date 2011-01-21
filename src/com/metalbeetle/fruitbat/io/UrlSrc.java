package com.metalbeetle.fruitbat.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/** Data source to read data via HTTP request from an URL. */
public class UrlSrc implements DataSrc {
	final URL url;
	public UrlSrc(URL url) {
		if (url == null) {
			throw new NullPointerException("UrlSrc object cannot be constructed with a null URL.");
		}
		this.url = url;
	}
	public String getName() { return url.getPath(); }
	public InputStream getInputStream() throws IOException { return url.openStream(); }
	@Override
	public String toString() { return url.toString(); }
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UrlSrc)) { return false; }
		return ((UrlSrc) o).url.equals(url);
	}
	@Override
	public int hashCode() { return 187 + 19 * url.hashCode(); }

	public long getLength() {
		return UNKNOWN_DATA_LENGTH;
	}
}
