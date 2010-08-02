package com.metalbeetle.fruitbat.storage;

/** Thrown when a store's configuration is invalid. */
public class StoreConfigInvalidException extends Exception {
	public StoreConfigInvalidException(String message) {
		super(message);
	}
}
