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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.ical.compat.javautil.DateIterator;
import com.google.ical.compat.javautil.DateIteratorFactory;
import com.google.ical.util.TimeUtils;
import com.google.ical.values.RRule;

public class RecurrenceScheduler {

	public static final String SCHEDULED_TRANSACTION_ID = "scheduledTransactionId";
	public static final String SCHEDULE_ALL = "scheduleAll";
	
	private final DateIterator ri;

	private RecurrenceScheduler(DateIterator ri) {
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

	public static RecurrenceScheduler create(RRule rrule) {
		return create(rrule, new Date());
	}

	public static RecurrenceScheduler create(RRule rrule, Date startDate) {
		DateIterator ri = DateIteratorFactory.createDateIterator(rrule, startDate, TimeUtils.utcTimezone());
		return new RecurrenceScheduler(ri);
	}

	public static ArrayList<TransactionInfo> getSortedSchedules(MyEntityManager em) {
		long t0 = System.currentTimeMillis();
		try {
			 ArrayList<TransactionInfo> list = em.getAllScheduledTransactions();
			 long now = calculateNextScheduleDate(list);
			 sortTransactionsByScheduleDate(list, now);
			 return list;
		} finally {
			Log.i("ScheduledListActivity", "getSortedSchedules ="+(System.currentTimeMillis()-t0)+"ms");
		}
	}

	private static final Date NULL_DATE = new Date(0);
	
	private static void sortTransactionsByScheduleDate(ArrayList<TransactionInfo> list, long now) {
		final Date today = new Date(now);
		Collections.sort(list, new Comparator<TransactionInfo>(){
			@Override
			public int compare(TransactionInfo o1, TransactionInfo o2) {
				Date d1 = o1 != null ? o1.nextDateTime : NULL_DATE;
				Date d2 = o2 != null ? o2.nextDateTime : NULL_DATE;
				if (d1 == null) {
					d1 = NULL_DATE;
				}
				if (d2 == null) {
					d2 = NULL_DATE;
				}
				if (d1.after(today)) {
					if (d2.after(today)) {
						return o1.nextDateTime.compareTo(o2.nextDateTime);
					} else {
						return -1;
					}
				} else {
					if (d2.after(today)) {
						return 1;
					} else {
						return -o1.nextDateTime.compareTo(o2.nextDateTime);
					}
				}
			}
		});
	}

	private static long calculateNextScheduleDate(ArrayList<TransactionInfo> list) {
		long now = System.currentTimeMillis();
		for (TransactionInfo t : list) {
			if (t.recurrence != null) {
				t.calculateNextDate(now);
			} else {
				t.nextDateTime = new Date(t.dateTime);
			}
		 }
		return now;
	}

	public static Date calculateNextDate(String recurrence) {
		return calculateNextDate(recurrence, System.currentTimeMillis());
	}
	
	public static Date calculateNextDate(String recurrence, long now) {
		Recurrence r = Recurrence.parse(recurrence);
		Date startDate = r.getStartDate().getTime();
		RRule rrule = r.createRRule();
		RecurrenceScheduler c = RecurrenceScheduler.create(rrule, startDate);
		c.advanceTo(new Date(now));
		if (c.hasNext()) {
			return c.next();
		} else {
			if (r.period.until != RecurrenceUntil.INDEFINETELY) {
				c = RecurrenceScheduler.create(rrule, startDate);
				Date nextDate = null;
				while (c.hasNext()) {
					nextDate = c.next();
				}
				return nextDate;
			} else {
				return null;
			}
		}
	}

	private static PendingIntent createPendingIntentForScheduledAlarm(Context context, TransactionInfo transaction) {
		Intent intent = new Intent("ru.orangesoftware.financisto.SCHEDULED_ALARM");
		intent.putExtra(SCHEDULED_TRANSACTION_ID, transaction.id);
		return PendingIntent.getBroadcast(context, (int)transaction.id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	public static void scheduleAll(Context context, ArrayList<TransactionInfo> transactions) {
		long now = System.currentTimeMillis();
		if (transactions != null && transactions.size() > 0) {
			for (TransactionInfo transaction : transactions) {
				scheduleAlarm(context, transaction, now);
			}
		}
	}

	public static boolean scheduleAlarm(Context context, TransactionInfo transaction, long now) {
		if (shouldSchedule(transaction, now)) {
			Date scheduleTime = transaction.nextDateTime; 
			AlarmManager service = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			PendingIntent pendingIntent = createPendingIntentForScheduledAlarm(context, transaction);
			service.set(AlarmManager.RTC_WAKEUP, scheduleTime.getTime(), pendingIntent);
			Log.i("RecurrenceScheduler", "Scheduling alarm for "+transaction.id+" at "+scheduleTime);
			return true;
		}
		return false;
	}

	private static boolean shouldSchedule(TransactionInfo transaction, long now) {
		return transaction.nextDateTime != null && now < transaction.nextDateTime.getTime();
	}

}
