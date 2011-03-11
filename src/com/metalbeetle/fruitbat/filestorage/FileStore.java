package com.metalbeetle.fruitbat.filestorage;

import com.metalbeetle.fruitbat.fulltext.FullTextIndex;
import com.metalbeetle.fruitbat.fulltext.LuceneIndex;
import com.metalbeetle.fruitbat.hierarchicalstorage.HSStore;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import com.metalbeetle.fruitbat.ProgressMonitor;
import java.io.File;

public class FileStore extends HSStore {
	final File f;
	final LuceneIndex luceneIndex;

	public FileStore(File f, ProgressMonitor pm) throws FatalStorageException {
		super(new FileLocation(f), pm);
		this.f = f;
		luceneIndex = new LuceneIndex(new File(f, "lucene-index"));
	}

	public FullTextIndex getFullTextIndex() { return luceneIndex; }

	public File getFile() { return f; }
}
