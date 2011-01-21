package com.metalbeetle.fruitbat.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents an object to which data can get written. Exposes a CommittableOutputStream, which may
 * allow transactional writing.
 */
public interface DataSink {
	/** @return A nonunique but hopefully informative name for the sink. */
	public String getName();
	/** @return Wrapper around output stream, which may be transactional, or may not. */
	public CommittableOutputStream getOutputStream() throws IOException;

	public interface CommittableOutputStream {
		public OutputStream stream() throws IOException;
		public void commitIfNotAborted() throws IOException;
		public void abort();
		/**
		 * If true the output stream is transactional: Can write data to stream and then abort.
		 */
		public boolean isAbortable();
	}
}
