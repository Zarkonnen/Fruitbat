package com.metalbeetle.fruitbat.storage;

public class RevisionNotFoundException extends Exception {
	public final String docID;
	public final String revision;

	public RevisionNotFoundException(String docID, String revision) {
		super("Revision " + revision + " of document with ID " + docID + " could not be found.");
		this.docID = docID;
		this.revision = revision;
	}
}
