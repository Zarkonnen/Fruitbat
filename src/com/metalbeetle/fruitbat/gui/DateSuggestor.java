package com.metalbeetle.fruitbat.gui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Misc.*;

class DateSuggestor implements TagSuggestor {
	public List<String> suggestSearchTerms(String[] currentTerms) {
		ArrayList<String> suggs = new ArrayList<String>();
		GregorianCalendar today = new GregorianCalendar();
		String year = string(today.get(Calendar.YEAR));
		String month = string(today.get(Calendar.MONTH) + 1);
		if (month.length() == 1) { month = "0" + month; }
		suggs.add("d:" + year);
		suggs.add("d:" + year + "-" + month);
		for (String t : currentTerms) { suggs.remove(t); }
		return suggs;
	}
}
