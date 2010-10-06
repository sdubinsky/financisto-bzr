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

import ru.orangesoftware.financisto.service.FinancistoService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PackageReplaceReciever extends BroadcastReceiver {

	private static final String PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("PackageReplaceReciever", "Recieved "+intent);
		String action = intent.getAction();
		if (PACKAGE_REPLACED.equals(action)) {
			FinancistoService.acquireLock(context);
			Intent serviceIntent = new Intent(context, FinancistoService.class);
			serviceIntent.putExtra(FinancistoService.SCHEDULE_ALL, true);
			context.startService(serviceIntent);			
		}
	}

}
