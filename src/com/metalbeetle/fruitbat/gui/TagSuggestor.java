package com.metalbeetle.fruitbat.gui;

import java.util.List;

public interface TagSuggestor {
	public List<String> suggestSearchTerms(String[] currentTerms);
}
