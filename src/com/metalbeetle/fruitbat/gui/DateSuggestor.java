package com.metalbeetle.fruitbat.gui;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import static com.metalbeetle.fruitbat.util.Misc.*;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class DateSuggestor implements TagSuggestor {
	public List<String> suggestSearchTerms(String[] currentTerms) {
		for (String t : currentTerms) {
			if (t.startsWith("d:")) {
				return Collections.<String>emptyList();
			}
		}
		GregorianCalendar today = new GregorianCalendar();
		String year = string(today.get(Calendar.YEAR));
		String month = string(today.get(Calendar.MONTH) + 1);
		if (month.length() == 1) { month = "0" + month; }
		return l("d:" + year, "d:" + year + "-" + month);
	}
}
