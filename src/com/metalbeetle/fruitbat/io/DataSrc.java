package com.metalbeetle.fruitbat.io;

import java.io.IOException;
import java.io.InputStream;

/** Represents a source of binary data. */
public interface DataSrc {
	static final long UNKNOWN_DATA_LENGTH = -7;
	/** @return A nonunique but hopefully informative name for the source. */
	public String getName();
	/** @return An InputStream of the data - a new one is created at each invocation! */
	public InputStream getInputStream() throws IOException;
	/** @return The length of data, or failing that, UNKNOWN_DATA_LENGTH. */
	public long getLength() throws IOException;
}
