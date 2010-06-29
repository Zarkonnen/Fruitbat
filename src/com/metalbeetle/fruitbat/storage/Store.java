package com.metalbeetle.fruitbat.storage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.metalbeetle.fruitbat.util.Collections.*;
import static com.metalbeetle.fruitbat.util.Misc.*;

public class Store {
	static final String _DELETED = "_deleted";
	static final String NEXT_DOC_ID = "next_doc_id";
	static final Map<String, String> META_DEFAULTS = m(p(NEXT_DOC_ID, "0"));

	final File location;
	final File docsF;
	final KVFile metaF;
	final ArrayList<Document> ds = new ArrayList<Document>();

	public Store(File location) {
		this.location = location;
		docsF = new File(location, "docs");
		metaF = new KVFile(new File(location, "meta.csv"), META_DEFAULTS);
		if (docsF.exists()) {
			for (File f : docsF.listFiles(new DocFilter())) {
				ds.add(new Document(f));
			}
		}
	}

	static final class DocFilter implements FilenameFilter {
		public boolean accept(File dir, String name) { return name.matches("[0-9]+"); }
	}
	
	public List<Document> docs() { return immute(ds); }
	
	public Document create() {
		try {
			Document d = new Document(new File(docsF, metaF.get(NEXT_DOC_ID)));
			metaF.put(NEXT_DOC_ID, string(integer(metaF.get(NEXT_DOC_ID)) + 1));
			mkDirs(d.location);
			ds.add(d);
			return d;
		} catch (Exception e) {
			throw new RuntimeException("Unable to create new document in " + location + ".", e);
		}
	}

	public void delete(Document d) {
		File delF = new File(docsF, d.location.getName() + _DELETED);
		if (!d.location.renameTo(delF)) {
			throw new RuntimeException("Couldn't delete document.\nUnable to move " + d.location + " to " +
					delF + ".");
		}
		ds.remove(d);
	}

	public Document undelete(String docID) {
		File delF = new File(docsF, docID + _DELETED);
		File f = new File(docsF, docID);
		if (!delF.renameTo(f)) {
			throw new RuntimeException("Couldn't uncelete document.\nUnable to move " + f + " to " +
					delF + ".");
		}
		Document d = new Document(f);
		ds.add(d);
		return d;
	}

	@Override
	public String toString() {
		return "store@" + location;
	}
}
