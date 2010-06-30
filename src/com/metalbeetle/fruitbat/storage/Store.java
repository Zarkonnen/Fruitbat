package com.metalbeetle.fruitbat.storage;

import java.util.List;

/** Interface for storing documents. */
public interface Store {
	public Document create();
	public void delete(Document d);
	public Document undelete(String docID);
	public List<Document> docs();
	public Document get(String id);
}
