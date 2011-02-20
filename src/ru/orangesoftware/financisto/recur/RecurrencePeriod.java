/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.recur;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;

import ru.orangesoftware.financisto.utils.DateUtils;

import com.google.ical.compat.javautil.DateIteratorFactory;
import com.google.ical.values.RRule;

public class RecurrencePeriod {

	public final RecurrenceUntil until;
	public final String params;
	
	public RecurrencePeriod(RecurrenceUntil until, String params) {
		this.until = until;
		this.params = params;
	}
	
	public static RecurrencePeriod noEndDate() {
		return new RecurrencePeriod(RecurrenceUntil.INDEFINETELY, null);
	}
	
	public static RecurrencePeriod empty(RecurrenceUntil until) {
		return new RecurrencePeriod(until, null);
	}

	public static RecurrencePeriod parse(String string) {
		String[] a = string.split(":");
		return new RecurrencePeriod(RecurrenceUntil.valueOf(a[0]), a[1]);
	}

	public String stateToString() {
		return until.name()+":"+params;
	}

	public void updateRRule(RRule r, Calendar startDate) {
		HashMap<String, String> state = RecurrenceViewFactory.parseState(params);
		switch (until) {
		case EXACTLY_TIMES:
			int count = Integer.parseInt(state.get(RecurrenceViewFactory.P_COUNT));
			r.setCount(count);
			break;
		case STOPS_ON_DATE:
			Calendar c = Calendar.getInstance();
			String stopsOnDate = state.get(RecurrenceViewFactory.P_DATE);
			try {
				c.setTime(DateUtils.FORMAT_DATE_RFC_2445.parse(stopsOnDate));
				c.set(Calendar.HOUR_OF_DAY, startDate.get(Calendar.HOUR_OF_DAY));
				c.set(Calendar.MINUTE, startDate.get(Calendar.MINUTE));
				c.set(Calendar.SECOND, startDate.get(Calendar.SECOND));
				c.set(Calendar.MILLISECOND, 0);
			} catch (ParseException e) {
				throw new IllegalArgumentException(params);
			}
			r.setUntil(DateIteratorFactory.dateToDateValue(c.getTime(), true));
			break;
		}
	}

}
