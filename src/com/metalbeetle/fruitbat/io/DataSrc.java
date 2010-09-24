package com.metalbeetle.fruitbat.io;

import java.io.IOException;
import java.io.InputStream;

/** Represents a source of binary data. */
public interface DataSrc {
	/** @return A nonunique but hopefully informative name for the source. */
	public String getName();
	/** @return An InputStream of the data - a new one is created at each invocation! */
	public InputStream getInputStream() throws IOException;
}
