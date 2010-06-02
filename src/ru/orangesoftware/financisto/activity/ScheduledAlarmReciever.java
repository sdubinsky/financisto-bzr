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
package ru.orangesoftware.financisto.activity;

import ru.orangesoftware.financisto.recur.RecurrenceScheduler;
import ru.orangesoftware.financisto.service.FinancistoService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScheduledAlarmReciever extends BroadcastReceiver {

	private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("ScheduledAlarmReciever", "Recieved "+intent);
		FinancistoService.acquireLock(context);
		Intent serviceIntent = new Intent(context, FinancistoService.class);
		String action = intent.getAction();
		if (BOOT_COMPLETED.equals(action)) {
			serviceIntent.putExtra(RecurrenceScheduler.SCHEDULE_ALL, true);
		} else {
			serviceIntent.putExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1));
		}
		context.startService(serviceIntent);			
	}

}
