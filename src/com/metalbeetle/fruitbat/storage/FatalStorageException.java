package com.metalbeetle.fruitbat.storage;

/** Thrown when a store is unable to recover from a read/write failure. */
public class FatalStorageException extends Exception {
	public FatalStorageException(String message) {
		super(message);
	}

	public FatalStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
