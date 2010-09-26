package com.metalbeetle.fruitbat.fulltext;

import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.util.List;

public interface FullTextIndex {
	public List<Document> query(List<List<String>> phrases, List<Document> within) throws FatalStorageException;
	public void pageAdded(DataSrc text, Document doc) throws FatalStorageException;
	public void pageRemoved(DataSrc text, Document doc) throws FatalStorageException;
}
