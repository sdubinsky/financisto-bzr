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

import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.SystemAttribute;
import ru.orangesoftware.financisto.model.TransactionAttributeInfo;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.recur.NotificationOptions;
import ru.orangesoftware.financisto.recur.RecurrenceScheduler;
import ru.orangesoftware.financisto.utils.MyPreferences;
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
					if (intent.getBooleanExtra(RecurrenceScheduler.SCHEDULE_ALL, false)) {
						scheduleAll(this, em);
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

	public static void scheduleAll(Context context, MyEntityManager em) {
		ArrayList<TransactionInfo> transactions = RecurrenceScheduler.getSortedSchedules(em);
		RecurrenceScheduler.scheduleAll(context, transactions);
	}

	private void scheduleOne(Intent intent) {
		long scheduledTransactionId = intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1);
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
			transaction.calculateNextDate(now);
			return RecurrenceScheduler.scheduleAlarm(this, transaction, now);
		}
		return false;
	}

	private long duplicateTransactionFromTemplate(TransactionInfo transaction) {
		return db.duplicateTransaction(transaction.id);
	}

	private void notifyUser(TransactionInfo transaction, long transactionId) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = createNotification(transaction, transactionId);
		notificationManager.notify((int)transactionId, notification);
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
		em = new MyEntityManager(this, db.db());
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
