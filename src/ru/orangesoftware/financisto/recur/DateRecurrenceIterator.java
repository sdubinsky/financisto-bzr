package ru.orangesoftware.financisto.recur;

import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.RRule;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import static ru.orangesoftware.financisto.recur.RecurrencePeriod.dateToDateValue;
import static ru.orangesoftware.financisto.recur.RecurrencePeriod.dateValueToDate;

public class DateRecurrenceIterator {

	private final RecurrenceIterator ri;

	private DateRecurrenceIterator(RecurrenceIterator ri) {
		this.ri = ri;
	}

	public boolean hasNext() {
		return ri.hasNext();
	}

	public Date next() {
		return dateValueToDate(ri.next());
	}

	public static DateRecurrenceIterator create(RRule rrule, Date startDate) throws ParseException {
        RecurrenceIterator ri = RecurrenceIteratorFactory.createRecurrenceIterator(rrule,
                dateToDateValue(startDate), TimeUtils.utcTimezone());
		return new DateRecurrenceIterator(ri);
	}

    public static DateRecurrenceIterator empty() {
        return new EmptyDateRecurrenceIterator();
    }

    private static class EmptyDateRecurrenceIterator extends DateRecurrenceIterator {
        public EmptyDateRecurrenceIterator() {
            super(null);
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Date next() {
            return null;
        }
    }
}
