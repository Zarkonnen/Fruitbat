package com.metalbeetle.fruitbat.fulltext;

import com.metalbeetle.fruitbat.io.DataSrc;
import com.metalbeetle.fruitbat.storage.Document;
import com.metalbeetle.fruitbat.storage.FatalStorageException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import static com.metalbeetle.fruitbat.util.Misc.*;

public class LuceneIndex implements FullTextIndex {
	final File indexF;
	final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30,
			/* stop words */ Collections.emptySet());
	final Directory directory;
	final IndexWriter writer;

	public LuceneIndex(File indexF) throws FatalStorageException {
		try {
			this.indexF = indexF;
			directory = new SimpleFSDirectory(indexF, NoLockFactory.getNoLockFactory());
			writer = new IndexWriter(directory, analyzer, /*create*/ !indexF.exists(),
					IndexWriter.MaxFieldLength.UNLIMITED);
		} catch (Exception e) {
			throw new FatalStorageException("Could not initialise lucene index for " + indexF + ".",
					e);
		}
	}

	public List<Document> query(List<List<String>> phrases, List<Document> within) throws FatalStorageException {
		try {
			IndexSearcher searcher = new IndexSearcher(writer.getReader());
			BooleanQuery bq = new BooleanQuery();
			for (List<String> phrase : phrases) {
				PhraseQuery pq = new PhraseQuery();
				for (String term : phrase) {
					pq.add(new Term("text", term.toLowerCase()));
				}
				bq.add(pq, Occur.MUST);
			}
			TopDocs td = searcher.search(bq, Integer.MAX_VALUE);
			if (td.totalHits == 0) { return Collections.emptyList(); }
			HashMap<Integer, Document> idToDoc = new HashMap<Integer, Document>();
			for (Document d : within) { idToDoc.put(d.getID(), d); }
			HashSet<Document> result = new HashSet<Document>();
			for (ScoreDoc sd : td.scoreDocs) {
				int id = integer(searcher.doc(sd.doc).get("docID"));
				if (idToDoc.containsKey(id)) {
					result.add(idToDoc.get(id));
				}
			}
			return new ArrayList<Document>(result);
		} catch (Exception e) {
			throw new FatalStorageException("Could not search full text index.", e);
		}
	}

	public void pageAdded(DataSrc text, Document doc) throws FatalStorageException {
		try {
			org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
			luceneDoc.add(new Field("docID", string(doc.getID()), Field.Store.YES,
					Field.Index.ANALYZED));
			String textS = srcToString(text);
			luceneDoc.add(new Field("text", textS, Field.Store.YES,
					Field.Index.ANALYZED));
			luceneDoc.add(new Field("hash", string(textS.hashCode()) + string(doc.getID()),
					Field.Store.YES, Field.Index.ANALYZED));
			writer.addDocument(luceneDoc);
			writer.commit();
		} catch (Exception e) {
			throw new FatalStorageException("Could not add page to full text index.", e);
		}
	}

	public void pageRemoved(DataSrc text, Document doc) throws FatalStorageException {
		try {
			String hash = string(srcToString(text).hashCode()) + string(doc.getID());
			writer.deleteDocuments(new Term("hash", hash));
			writer.commit();
		} catch (Exception e) {
			throw new FatalStorageException("Could not remove page from full text index.", e);
		}
	}

	public void close() {
		try {
			writer.close();
		} catch (Exception e) {
			// Um. Er. ?
		}
	}
}
