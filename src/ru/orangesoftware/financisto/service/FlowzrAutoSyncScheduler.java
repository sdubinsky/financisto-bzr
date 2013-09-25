/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/16/11 12:54 AM
 */
public class FlowzrAutoSyncScheduler {

    private final long now;

    public static void scheduleNextAutoSync(Context context) {
        //if (MyPreferences.isAutoSync(context)) {
    	if (true) {
            new FlowzrAutoSyncScheduler(System.currentTimeMillis() + (1*60*100)).scheduleSync(context);
        }
    }
    
    public FlowzrAutoSyncScheduler(long now) {
        this.now = now;
    }

    public void scheduleSync(Context context) {
        AlarmManager service = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = createPendingIntent(context);
        service.set(AlarmManager.RTC_WAKEUP, now, pendingIntent);
        Log.i("Financisto", "Next flowzr-sync scheduled at "+ String.valueOf(now));
    }

    private PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent("ru.orangesoftware.financisto.SCHEDULED_SYNC");
        return PendingIntent.getBroadcast(context, -100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public long getScheduledTime() {
        return now;
    }

}
