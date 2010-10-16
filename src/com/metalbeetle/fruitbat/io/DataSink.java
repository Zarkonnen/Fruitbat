package com.metalbeetle.fruitbat.io;

import java.io.IOException;
import java.io.OutputStream;

public interface DataSink {
	/** @return A nonunique but hopefully informative name for the sink. */
	public String getName();
	/** @return Wrapper around output stream, which may be transactional, or may not. */
	public CommittableOutputStream getOutputStream();

	public interface CommittableOutputStream {
		public OutputStream stream() throws IOException;
		public void commitIfNotAborted() throws IOException;
		public void abort();
	}
}
