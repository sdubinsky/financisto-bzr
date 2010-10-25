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

public class PackageReplaceReceiver extends BroadcastReceiver {

	private static final String PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("PackageReplaceReceiver", "Received "+intent);
		String action = intent.getAction();
		if (PACKAGE_REPLACED.equals(action)) {
			Intent serviceIntent = new Intent(FinancistoService.ACTION_SCHEDULE_ALL);
            FinancistoService.sendWakefulWork(context, serviceIntent);
		}
	}

}
