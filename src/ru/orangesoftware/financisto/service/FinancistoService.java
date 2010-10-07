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
package ru.orangesoftware.financisto.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.activity.MassOpActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.RestoredTransaction;
import ru.orangesoftware.financisto.model.SystemAttribute;
import ru.orangesoftware.financisto.model.TransactionAttributeInfo;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.recur.NotificationOptions;
import ru.orangesoftware.financisto.recur.RecurrenceScheduler;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.RemoteViews;

public class FinancistoService extends Service {

	private static final String TAG = "FinancistoService";
	public static final String WIDGET_UPDATE_ACTION = "ru.orangesoftware.financisto.UPDATE_WIDGET";
	public static final String SCHEDULED_TRANSACTION_ID = "scheduledTransactionId";
	public static final String SCHEDULE_ALL = "scheduleAll";

	private static final int RESTORED_NOTIFICATION_ID = 0;

	private DatabaseAdapter db;
	private MyEntityManager em;
	
	@Override
	public void onStart(Intent intent, int startId) {
		try {
			Log.d(TAG, "Started with "+intent);
			if (intent != null) {
				String action = intent.getAction();
				if (WIDGET_UPDATE_ACTION.equals(action)) {
					if (MyPreferences.isWidgetEnabled(this)) {
			        	long accountId = -1;
			        	if (intent != null) {
			        		accountId = intent.getLongExtra(AccountWidget.WIDGET_ACCOUNT_ID, -1);
			        		if (accountId == -1) {
			        			accountId = intent.getLongExtra(AccountWidget.TRANSACTION_ACCOUNT_ID, -1);
			        			if (accountId != -1) {
			        				updateWidget(AccountWidget.buildUpdatesForAccount(this, accountId));
			        				return;
			        			}
			        		}
			        	}
			        	updateWidget(AccountWidget.buildUpdate(this, accountId));
					} else {
			        	updateWidget(AccountWidget.noDataUpdate(this));						
					}
				} else {
					if (intent.getBooleanExtra(SCHEDULE_ALL, false)) {
						scheduleAll(this, db);
					} else {
						scheduleOne(intent);
					}
				}
			}
		} finally {
			releaseLock();
		}
	}

	private void updateWidget(RemoteViews updateViews) {
		// Push update for this widget to the home screen
		ComponentName thisWidget = new ComponentName(this, AccountWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		manager.updateAppWidget(thisWidget, updateViews);
	}

	public static void scheduleAll(Context context, DatabaseAdapter db) {		
		long now = System.currentTimeMillis();
		if (MyPreferences.isRestoreMissedScheduledTransactions(context)) {
			restoreMissedSchedules(context, db, now);
			// all transactions up to and including now has already been restored
			now += 1000;
		}
		scheduleAll(context, db, now);
	}

	private void scheduleOne(Intent intent) {
		long scheduledTransactionId = intent.getLongExtra(SCHEDULED_TRANSACTION_ID, -1);
		Log.i(TAG, "Alarm for "+scheduledTransactionId+" recieved..");
		TransactionInfo transaction = em.getTransactionInfo(scheduledTransactionId);
		if (transaction != null) {
			long transactionId = duplicateTransactionFromTemplate(transaction);
			notifyUser(transaction, transactionId);
			boolean hasBeenRescheduled = rescheduleTransaction(transaction);
			if (!hasBeenRescheduled) {
				deleteTransactionIfNeeded(transaction);
				Log.i("FinancistoService", "Expired transaction "+transaction.id+" has been deleted");
			}
			updateWidget(this);
		}
	}

	private void deleteTransactionIfNeeded(TransactionInfo transaction) {
		TransactionAttributeInfo a = em.getSystemAttributeForTransaction(SystemAttribute.DELETE_AFTER_EXPIRED, transaction.id);
		if (a != null && Boolean.valueOf(a.value)) {
			db.deleteTransaction(transaction.id);
		}
	}

	private boolean rescheduleTransaction(TransactionInfo transaction) {
		if (transaction.recurrence != null) {
			long now = System.currentTimeMillis()+1000;
			RecurrenceScheduler.calculateNextDate(transaction, now);
			return scheduleAlarm(this, transaction, now);
		}
		return false;
	}

	public static void scheduleAll(Context context, DatabaseAdapter db, long now) {
		ArrayList<TransactionInfo> scheduled = RecurrenceScheduler.getSortedSchedules(db.em(), now);
		scheduleAll(context, scheduled, now);
}
	
	public static void scheduleAll(Context context, ArrayList<TransactionInfo> transactions, long now) {
		if (transactions != null && transactions.size() > 0) {
			for (TransactionInfo transaction : transactions) {
				scheduleAlarm(context, transaction, now);
			}
		}
	}

	/**
	 * Restores missed scheduled transactions on backup and on phone restart
	 * @param context context
	 * @param db db
	 * @param now current time
	 */
	private static void restoreMissedSchedules(Context context, DatabaseAdapter db, long now) {
		try {
			List<RestoredTransaction> restored = RecurrenceScheduler.restoreMissedSchedules(db.em(), now);
			if (restored.size() > 0) {
				db.storeMissedSchedules(restored, now);
				Log.i("Financisto", "["+restored.size()+"] scheduled transactions have been restored:");
				for (int i=0; i<10 && i<restored.size(); i++) {
					RestoredTransaction rt = restored.get(i);
					Log.i("Financisto", rt.transactionId+" at "+rt.dateTime);				
				}
				context.getSystemService(NOTIFICATION_SERVICE);
				notifyUser(context, RESTORED_NOTIFICATION_ID, createRestoredNotification(context, restored.size()));
			}
		} catch (Exception ex) {
			// eat all exceptions
			Log.e("Financisto", "Unexpected error while restoring schedules", ex);			
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

	private static PendingIntent createPendingIntentForScheduledAlarm(Context context, TransactionInfo transaction) {
		Intent intent = new Intent("ru.orangesoftware.financisto.SCHEDULED_ALARM");
		intent.putExtra(SCHEDULED_TRANSACTION_ID, transaction.id);
		return PendingIntent.getBroadcast(context, (int)transaction.id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
		
	private long duplicateTransactionFromTemplate(TransactionInfo transaction) {
		return db.duplicateTransaction(transaction.id);
	}

	private void notifyUser(TransactionInfo transaction, long transactionId) {
		Notification notification = createNotification(transaction, transactionId);
		notifyUser(this, (int)transactionId, notification);
	}

	private static void notifyUser(Context context, int id, Notification notification) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(id, notification);		
	}

	private static Notification createRestoredNotification(Context context, int count) {
		long when = System.currentTimeMillis();
		String text = context.getString(R.string.scheduled_transactions_have_been_restored, count);
		Notification notification = new Notification(R.drawable.notification_icon_transaction, text, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults = Notification.DEFAULT_ALL;
		Intent notificationIntent = new Intent(context, MassOpActivity.class);
		WhereFilter filter = new WhereFilter("");
		filter.eq(BlotterFilter.STATUS, TransactionStatus.RS.name());
		filter.toIntent(notificationIntent);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, context.getString(R.string.scheduled_transactions_restored), text, contentIntent);	
		return notification;
	}

	private Notification createNotification(TransactionInfo t, long transactionId) {
		long when = System.currentTimeMillis();
		Notification notification = new Notification(t.getNotificationIcon(), t.getNotificationTickerText(this), when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		applyNotificationOptions(notification, t.notificationOptions);
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, t.getActivity());
		notificationIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, transactionId);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, t.getNotificationContentTitle(this), t.getNotificationContentText(this), contentIntent);	
		return notification;
	}

	private void applyNotificationOptions(Notification notification, String notificationOptions) {
		if (notificationOptions == null) {
			notification.defaults = Notification.DEFAULT_ALL;
		} else {
			NotificationOptions options = NotificationOptions.parse(notificationOptions);
			options.apply(notification);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		db = new DatabaseAdapter(this);
		db.open();
		em = db.em();
		Log.d(TAG, "Created..");
	}

	@Override
	public void onDestroy() {
		if (db != null) {
			db.close();
		}
		Log.d(TAG, "Destroyed..");
		super.onDestroy();
	}



	private static WakeLock sWakeLock;
	
	public static synchronized void acquireLock(Context context) {
		Log.d(TAG, Thread.currentThread().getName()+" WakeLock is about to be acquired "+sWakeLock);
		if (sWakeLock != null) {
			return;
		}
		PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);	
		sWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "MissedReminderWakeLock");
		sWakeLock.acquire();
		Log.d(TAG, "WakeLock acquired "+sWakeLock);
	}
	
	public static synchronized void releaseLock() {
		Log.d(TAG, Thread.currentThread().getName()+" WakeLock is about to be released "+sWakeLock);
		if (sWakeLock != null) {
			sWakeLock.release();
			sWakeLock = null;
			Log.d(TAG, "WakeLock released "+sWakeLock);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public static void updateWidget(Context context) {
		Intent intent = new Intent(context, FinancistoService.class);
		intent.setAction(WIDGET_UPDATE_ACTION);
		context.startService(intent);
	}

}
