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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.R.drawable;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.R.xml;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);   
		addPreferencesFromResource(R.xml.preferences);	
		final Preference pNewTransactionShortcut = getPreferenceScreen().findPreference("shortcut_new_transaction");
		pNewTransactionShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				addShortcut(".TransactionActivity", R.string.transaction, R.drawable.icon_transaction);
				return true;
			}
			
		});
		final Preference pNewTransferShortcut = getPreferenceScreen().findPreference("shortcut_new_transfer");
		pNewTransferShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				addShortcut(".TransferActivity", R.string.transfer, R.drawable.icon_transfer);
				return true;
			}
			
		});
	}
	
	private void addShortcut(String activity, int nameId, int iconId) {
		Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(this, iconId), 
				"com.android.launcher.action.INSTALL_SHORTCUT");
		sendBroadcast(intent);
	}

	private void removeShortcut(String activity, int nameId, int iconId) {
		Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(this, iconId), 
				"com.android.launcher.action.UNINSTALL_SHORTCUT");
		sendBroadcast(intent);
	}
	
	private Intent createShortcutIntent(String activity, String shortcutName, ShortcutIconResource shortcutIcon, String action) {
		Intent shortcutIntent = new Intent();
		shortcutIntent.setComponent(new ComponentName(this.getPackageName(), activity));
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		final Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon);
		intent.setAction(action);
		return intent;
	}

}
