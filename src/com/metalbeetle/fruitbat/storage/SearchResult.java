package com.metalbeetle.fruitbat.storage;

import java.util.List;

public final class SearchResult {
	public final List<Document> docs;
	public final List<String> narrowingTags;
	public final SearchOutcome outcome;
	public final int minimumAvailableDocs;

	public SearchResult(List<Document> docs, List<String> narrowingTags, SearchOutcome outcome, int minimumAvailableDocs) {
		this.docs = docs;
		this.narrowingTags = narrowingTags;
		this.outcome = outcome;
		this.minimumAvailableDocs = minimumAvailableDocs;
	}
}
