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
package ru.orangesoftware.financisto.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ru.orangesoftware.financisto.R;
import android.content.Context;
import android.provider.Settings;

public class DateUtils {
	
	public static final DateFormat FORMAT_TIMESTAMP_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static final DateFormat FORMAT_DATE_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateFormat FORMAT_TIME_ISO_8601 = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat FORMAT_DATE_RFC_2445 = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
	
	private static final Calendar c = Calendar.getInstance();	

	public static enum PeriodType {		
		TODAY(R.string.period_today), 
		YESTERDAY(R.string.period_yesterday), 
		THIS_WEEK(R.string.period_this_week), 
		THIS_MONTH(R.string.period_this_month),
		LAST_WEEK(R.string.period_last_week),
		LAST_MONTH(R.string.period_last_month),
		CUSTOM(R.string.period);
		
		public final int titleId;
		
		private PeriodType(int titleId) {
			this.titleId = titleId;
		}
	}
	
	public static class Period {
		
		public PeriodType type;
		public long start;
		public long end;
		
		public Period(PeriodType type, long start, long end) {
			this.type = type;
			this.start = start;
			this.end = end;
		}

		public boolean theSame(long start, long end) {
			return this.start == start && this.end == end;
		}

		public boolean isCustom() {
			return type == PeriodType.CUSTOM;
		}
		
	}
	
	public static Period today() {
		c.setTimeInMillis(System.currentTimeMillis());
		long start = startOfDay(c).getTimeInMillis();
		long end = endOfDay(c).getTimeInMillis();
		return new Period(PeriodType.TODAY, start, end);
	}

	public static Period yesterday() {
		c.setTimeInMillis(System.currentTimeMillis());
		c.add(Calendar.DAY_OF_MONTH, -1);
		long start = startOfDay(c).getTimeInMillis();
		long end = endOfDay(c).getTimeInMillis();
		return new Period(PeriodType.YESTERDAY, start, end);		
	}

	public static Period thisWeek() {
		c.setTimeInMillis(System.currentTimeMillis());
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		long start, end;
		if (dayOfWeek == Calendar.MONDAY) {
			start = startOfDay(c).getTimeInMillis();
			c.add(Calendar.DAY_OF_MONTH, 6);
			end = endOfDay(c).getTimeInMillis();
		} else {
			c.add(Calendar.DAY_OF_MONTH, -(dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek-2));
			start = startOfDay(c).getTimeInMillis();
			c.add(Calendar.DAY_OF_MONTH, 6);
			end = endOfDay(c).getTimeInMillis();
		}
		return new Period(PeriodType.THIS_WEEK, start, end);
	}

	public static Period lastWeek() {
		c.setTimeInMillis(System.currentTimeMillis());
		c.add(Calendar.DAY_OF_YEAR, -7);
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		long start, end;
		if (dayOfWeek == Calendar.MONDAY) {
			start = startOfDay(c).getTimeInMillis();
			c.add(Calendar.DAY_OF_MONTH, 6);
			end = endOfDay(c).getTimeInMillis();
		} else {
			c.add(Calendar.DAY_OF_MONTH, -(dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek-2));
			start = startOfDay(c).getTimeInMillis();
			c.add(Calendar.DAY_OF_MONTH, 6);
			end = endOfDay(c).getTimeInMillis();
		}
		return new Period(PeriodType.LAST_WEEK, start, end);
	}

	public static Period thisMonth() {
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(Calendar.DAY_OF_MONTH, 1);
		long start = startOfDay(c).getTimeInMillis();
		c.add(Calendar.MONTH, 1);
		c.add(Calendar.DAY_OF_MONTH, -1);
		long end = endOfDay(c).getTimeInMillis();
		return new Period(PeriodType.THIS_MONTH, start, end);
	}

	public static Period lastMonth() {
		c.setTimeInMillis(System.currentTimeMillis());
		c.add(Calendar.MONTH, -1);
		c.set(Calendar.DAY_OF_MONTH, 1);
		long start = startOfDay(c).getTimeInMillis();
		c.add(Calendar.MONTH, 1);
		c.add(Calendar.DAY_OF_MONTH, -1);
		long end = endOfDay(c).getTimeInMillis();
		return new Period(PeriodType.LAST_MONTH, start, end);
	}

	public static Period getPeriod(PeriodType period) {
		switch (period) {
		case TODAY:
			return today();
		case YESTERDAY:
			return yesterday();
		case THIS_WEEK:
			return thisWeek();
		case THIS_MONTH:
			return thisMonth();
		case LAST_WEEK:
			return lastWeek();
		case LAST_MONTH:
			return lastMonth();
		}
		return null;
	}

	public static Calendar startOfDay(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c;
	}

	public static Calendar endOfDay(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		c.set(Calendar.MILLISECOND, 999);
		return c;
	}

    public static long atMidnight(long date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return startOfDay(c).getTimeInMillis();
    }

	public static Date atDateAtTime(long now, Calendar startDate) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(now);
		c.set(Calendar.HOUR_OF_DAY, startDate.get(Calendar.HOUR_OF_DAY));
		c.set(Calendar.MINUTE, startDate.get(Calendar.MINUTE));
		c.set(Calendar.SECOND, startDate.get(Calendar.SECOND));
		c.set(Calendar.MILLISECOND, startDate.get(Calendar.MILLISECOND));
		return c.getTime();
	}

	public static DateFormat getShortDateFormat(Context context) {
		return android.text.format.DateFormat.getDateFormat(context);
	}

	public static DateFormat getLongDateFormat(Context context) {
		return android.text.format.DateFormat.getLongDateFormat(context);
	}

	public static DateFormat getMediumDateFormat(Context context) {
		return android.text.format.DateFormat.getMediumDateFormat(context);
	}

	public static DateFormat getTimeFormat(Context context) {
		return android.text.format.DateFormat.getTimeFormat(context);
	}

	public static boolean is24HourFormat(Context context) {
		return "24".equals(Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24));
	}

	public static void zeroSeconds(Calendar dateTime) {
		dateTime.set(Calendar.SECOND, 0);
		dateTime.set(Calendar.MILLISECOND, 0);
	}
}
