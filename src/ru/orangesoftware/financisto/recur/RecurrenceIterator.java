package ru.orangesoftware.financisto.recur;

import java.util.Date;

import com.google.ical.compat.javautil.DateIterator;
import com.google.ical.compat.javautil.DateIteratorFactory;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.RRule;

public class RecurrenceIterator {

	private final DateIterator ri;

	private RecurrenceIterator(DateIterator ri) {
		this.ri = ri;
	}

	public boolean hasNext() {
		return ri.hasNext();
	}

	public Date next() {
		return ri.next();
	}

	public void advanceTo(Date date) {
		ri.advanceTo(date);
	}

	public static RecurrenceIterator create(RRule rrule, Date startDate) {
		DateIterator ri = DateIteratorFactory.createDateIterator(rrule, startDate, TimeUtils.utcTimezone());
		return new RecurrenceIterator(ri);
	}

}
