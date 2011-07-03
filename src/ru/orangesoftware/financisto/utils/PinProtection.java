/*******************************************************************************
 * Copyright (c) 2011 Timo Lindhorst
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Timo Lindhorst - Initial implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.utils;


import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import ru.orangesoftware.financisto.activity.PinActivity;

public class PinProtection {
	private static long lockTime = 0;
	private static boolean unlocked = false;
	private static final int MIN_DELTA_TIME_MS = 3000;
	
	private static void askForPin(Context c) {
		Intent intent = new Intent(c, PinActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		c.startActivity(intent);
	}

	public static void lock(Context c) {
		if (unlocked) {
			lockTime = System.currentTimeMillis();
		}
	}

	public static void unlock(Context c) {
		long curTime = System.currentTimeMillis();
		long deltaTimeMs = TimeUnit.MILLISECONDS.convert(MyPreferences.getLockTimeSeconds(c), TimeUnit.SECONDS);
		if (deltaTimeMs < MIN_DELTA_TIME_MS)
			deltaTimeMs = MIN_DELTA_TIME_MS;
		if ((curTime - lockTime) < deltaTimeMs) {
			unlocked = true;
		} else if (! MyPreferences.isPinProtected(c)) {
			unlocked = true;
			lockTime = System.currentTimeMillis();
		} else if (! MyPreferences.isPinLockEnabled(c)) {
			unlocked = true;
			lockTime = System.currentTimeMillis();
			MyPreferences.setPinLockEnabled(c, true);
		} else {
			askForPin(c);
		}
	}
}
