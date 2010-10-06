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
import java.util.List;

import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.RestoredTransaction;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import android.util.Log;

import com.google.ical.values.RRule;

public class RecurrenceScheduler {

	private static final Date NULL_DATE = new Date(0);
	private static final int MAX_RESTORED = 1000;
	
	public static List<RestoredTransaction> restoreMissedSchedules(MyEntityManager em, long now) {
		long t0 = System.currentTimeMillis();
		try {
			Date endDate = new Date(now);
			List<RestoredTransaction> restored = new ArrayList<RestoredTransaction>();
			ArrayList<TransactionInfo> list = em.getAllScheduledTransactions();
			for (TransactionInfo t : list) {
				if (t.recurrence != null) {
					long lastRecurrence = t.lastRecurrence;
					if (lastRecurrence > 0) {
						RecurrenceIterator ri = createIterator(t.recurrence, lastRecurrence);
						while (ri.hasNext()) {
							Date nextDate = ri.next();
							if (nextDate.after(endDate)) {
								break;
							}
							addRestoredTransaction(restored, t, nextDate);
						}
					}
				} else {
					Date nextDate = new Date(t.dateTime);
					if (nextDate.before(endDate)) {
						addRestoredTransaction(restored, t, nextDate);
					}
				}				
			}
			if (restored.size() > MAX_RESTORED) {
				Collections.sort(restored, new Comparator<RestoredTransaction>(){
					@Override
					public int compare(RestoredTransaction t0, RestoredTransaction t1) {
						return t1.dateTime.compareTo(t0.dateTime);
					}
				});
				restored = restored.subList(0, MAX_RESTORED);
			}
			return restored;
		} finally {
			Log.i("ScheduledListActivity", "getSortedSchedules ="+(System.currentTimeMillis()-t0)+"ms");
		}		
	}

	private static void addRestoredTransaction(List<RestoredTransaction> restored, 
			TransactionInfo t, Date nextDate) {
		RestoredTransaction rt = new RestoredTransaction(t.id, nextDate);
		restored.add(rt);							
	}

	public static ArrayList<TransactionInfo> getSortedSchedules(MyEntityManager em, long now) {
		long t0 = System.currentTimeMillis();
		try {
			 ArrayList<TransactionInfo> list = em.getAllScheduledTransactions();
			 calculateNextScheduleDate(list, now);
			 sortTransactionsByScheduleDate(list, now);
			 return list;
		} finally {
			Log.i("ScheduledListActivity", "getSortedSchedules ="+(System.currentTimeMillis()-t0)+"ms");
		}
	}

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

	private static long calculateNextScheduleDate(ArrayList<TransactionInfo> list, long now) {
		for (TransactionInfo t : list) {
			if (t.recurrence != null) {
				calculateNextDate(t, now);
			} else {
				t.nextDateTime = new Date(t.dateTime);
			}
		 }
		return now;
	}

	public static Date calculateNextDate(TransactionInfo transaction, long now) {
		return transaction.nextDateTime = calculateNextDate(transaction.recurrence, now);
	}
	
	public static Date calculateNextDate(String recurrence, long now) {
		RecurrenceIterator ri = createIterator(recurrence, now);
		if (ri.hasNext()) {
			return ri.next();
		} else {
			return null;
		}
	}

	private static RecurrenceIterator createIterator(String recurrence, long now) {
		Recurrence r = Recurrence.parse(recurrence);
		Date startDate = r.getStartDate().getTime();
		RRule rrule = r.createRRule();
		RecurrenceIterator c = RecurrenceIterator.create(rrule, startDate);
		Date advanceDate = new Date(now);
		c.advanceTo(advanceDate);
		return c;
	}

}
