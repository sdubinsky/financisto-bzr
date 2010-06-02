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
import java.util.Date;
import java.util.HashMap;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.DateUtils;
import android.content.Context;

import com.google.ical.values.RRule;

public class Recurrence {

	private Calendar startDate;
	public RecurrencePattern pattern;
	public RecurrencePeriod period;
	
	public static Recurrence parse(String recurrence) {
		Recurrence r = new Recurrence();
		String[] a = recurrence.split("~");
		try {
			Date d = DateUtils.FORMAT_DATE_ISO_8601.parse(a[0]);
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			r.startDate = c;
		} catch (ParseException e) {
			throw new RuntimeException(recurrence);
		}
		r.pattern = RecurrencePattern.parse(a[1]);
		r.period = RecurrencePeriod.parse(a[2]);
		return r;
	}
	
	public String stateToString() {
		StringBuilder sb = new StringBuilder();
		sb.append(DateUtils.FORMAT_DATE_ISO_8601.format(startDate.getTime())).append("~");
		sb.append(pattern.stateToString()).append("~");
		sb.append(period.stateToString());
		return sb.toString();
	}	

	public static Recurrence noRecur() {
		Recurrence r = new Recurrence();
		r.startDate = Calendar.getInstance();
		r.pattern = RecurrencePattern.noRecur();
		r.period = RecurrencePeriod.noEndDate();
		return r;
		
	}
	
	public Calendar getStartDate() {
		return startDate;
	}
	
	public void updateStartDate(int y, int m, int d) {
		startDate.set(Calendar.YEAR, y);
		startDate.set(Calendar.MONTH, m);
		startDate.set(Calendar.DAY_OF_MONTH, d);
	}

	public void updateStartTime(int h, int m, int s) {
		startDate.set(Calendar.HOUR_OF_DAY, h);
		startDate.set(Calendar.MINUTE, m);
		startDate.set(Calendar.SECOND, s);
		startDate.set(Calendar.MILLISECOND, 0);
	}

	public RRule createRRule() {
		if (pattern.frequency == RecurrenceFrequency.GEEKY) {
			try {
				HashMap<String, String> map = RecurrenceViewFactory.parseState(pattern.params);
				String rrule = map.get(RecurrenceViewFactory.P_INTERVAL);
				return new RRule("RRULE:"+rrule);
			} catch (ParseException e) {
				throw new IllegalArgumentException(pattern.params);
			}
		} else {
			RRule r = new RRule();
			pattern.updateRRule(r);
			period.updateRRule(r, startDate);
			return r;
		}
	}

	public String toInfoString(Context context) {
		StringBuilder sb = new StringBuilder();
		sb.append(context.getString(pattern.frequency.titleId))
		  .append(", ").append(context.getString(R.string.recur_repeat_starts_on)).append(": ")
		  .append(DateUtils.getShortDateFormat(context).format(startDate.getTime())).append(" ")
		  .append(DateUtils.getTimeFormat(context).format(startDate.getTime()));
		return sb.toString();
	}

}
