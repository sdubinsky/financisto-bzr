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
package ru.orangesoftware.financisto.utils;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

public class MyPreferences {
	
	public static enum AccountSortOrder {
		SORT_ORDER_ASC("sortOrder", true),
		SORT_ORDER_DESC("sortOrder", false),
		NAME("title", true);
		
		public final String property;
		public final boolean asc;
		
		private AccountSortOrder(String property, boolean asc) {
			this.property = property;
			this.asc = asc;
		}
	}
	
	public static enum LocationsSortOrder {
		FREQUENCY("count", false),
		NAME("name", true);
		
		public final String property;
		public final boolean asc;
		
		private LocationsSortOrder(String property, boolean asc) {
			this.property = property;
			this.asc = asc;
		}
	}

	public static boolean isPinRequired = true;

	public static boolean isUseGps(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("use_gps", true);
	}
	
	public static boolean isUseMylocation(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("use_my_location", true);
	}

	public static boolean isPinProtected(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("pin_protection", false) && isPinRequired;
	}

	public static String getPin(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString("pin", null);
	}
	
	public static void setPinRequired(boolean isPinRequired) {
		MyPreferences.isPinRequired = isPinRequired;
	}

	public static AccountSortOrder getAccountSortOrder(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String sortOrder = sharedPreferences.getString("sort_accounts", AccountSortOrder.SORT_ORDER_DESC.name());
		return AccountSortOrder.valueOf(sortOrder);
	}

	public static LocationsSortOrder getLocationsSortOrder(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String sortOrder = sharedPreferences.getString("sort_locations", LocationsSortOrder.NAME.name());
		return LocationsSortOrder.valueOf(sortOrder);
	}

	public static long getLastAccount(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getLong("last_account_id", -1);
	}
	
	public static void setLastAccount(Context context, long accountId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		sharedPreferences.edit().putLong("last_account_id", accountId).commit();
	}

	public static boolean isRememberAccount(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("remember_last_account", true);
	}

	public static boolean isRememberCategory(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("remember_last_category", false);
	}

	public static boolean isRememberLocation(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("remember_last_location", false);
	}

	public static boolean isRememberProject(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("remember_last_project", false);
	}

	public static boolean isShowLocation(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("ntsl_show_location", true);
	}

	public static int getLocationOrder(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return Integer.parseInt(sharedPreferences.getString("ntsl_show_location_order", "1"));
	}
	
	/**
	 * Get google docs user login registered on preferences
	 * */
	public static String getUserLogin(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString("user_login", null);
	}

	/**
	 * Get google docs user password registered on preferences
	 * */
	public static String getUserPassword(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString("user_password", null);
	}

	/**
	 * Get google docs backup folder registered on preferences
	 * */
	public static String getBackupFolder(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString("backup_folder", null);
	}

	public static boolean isShowNote(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("ntsl_show_note", true);
	}

	public static int getNoteOrder(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return Integer.parseInt(sharedPreferences.getString("ntsl_show_note_order", "3"));
	}

	public static boolean isShowProject(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("ntsl_show_project", true);
	}

	public static int getProjectOrder(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return Integer.parseInt(sharedPreferences.getString("ntsl_show_project_order", "4"));
	}

	public static boolean isUseFixedLayout(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("ntsl_use_fixed_layout", true);
	}
	
	public static boolean isSendErrorReport(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("send_error_reports", true);
	}	
	
	public static boolean isWidgetEnabled(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("enable_widget", true);
	}	
	
	public static boolean isIncludeTransfersIntoReports(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("include_transfers_into_reports", false);
	}	
	
	private static final String DEFAULT = "default";
	
	public static void switchLocale(Context context, String locale) {
		if (DEFAULT.equals(locale)) {
			switchLocale(context, Locale.getDefault());
		} else {
			String[] a = locale.split("-");
			String language = a[0];
			String country = a.length > 1 ? a[1] : null;
			Locale newLocale = country != null ? new Locale(language, country) : new Locale(language);
			switchLocale(context, newLocale);			
		}
	}
	
	private static void switchLocale(Context context, Locale locale) {
    	Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = locale;
        Log.i("MyPreferences", "Switching locale to "+conf.locale.getDisplayName());
        res.updateConfiguration(conf, dm);
	}

}
