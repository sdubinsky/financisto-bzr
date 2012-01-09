/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Rodrigo Sousa - google docs backup
 *     Abdsandryk Souza - report preferences
 ******************************************************************************/
package ru.orangesoftware.financisto.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.Currency;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import static ru.orangesoftware.financisto.utils.AndroidUtils.isSupportedApiLevel;

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

    private static Method hasSystemFeatureMethod;

    static {
        // hack for 1.5/1.6 devices
        try {
            hasSystemFeatureMethod = PackageManager.class.getMethod("hasSystemFeature", new Class[] { String.class } );
       } catch (NoSuchMethodException ex) {
            hasSystemFeatureMethod = null;
       }

    }

	public static boolean isUseGps(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("use_gps", true);
	}
	
	public static boolean isUseMyLocation(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("use_my_location", true);
	}

    public static boolean isPinProtected(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("pin_protection", false);
    }

    public static boolean isPinProtectedNewTransaction(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("pin_protection_lock_transaction", true);
    }

    public static boolean isPinLockEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return isPinProtected(context) && sharedPreferences.getBoolean("pin_protection_lock", true);
    }

    public static int getLockTimeSeconds(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return isPinLockEnabled(context) ? 60*Integer.parseInt(sharedPreferences.getString("pin_protection_lock_time", "5")) : 0;
    }

	public static String getPin(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString("pin", null);
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

    public static boolean isShowTakePicture(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return isCameraSupported(context) && sharedPreferences.getBoolean("ntsl_show_picture", true);
    }

    public static boolean isShowCategoryInTransferScreen(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("ntsl_show_category_in_transfer", true);
    }

    public static boolean isShowPayee(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("ntsl_show_payee", true);
    }

    public static int getPayeeOrder(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(sharedPreferences.getString("ntsl_show_payee_order", "1"));
    }

	public static boolean isShowLocation(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return isLocationSupported(context) && sharedPreferences.getBoolean("ntsl_show_location", true);
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
	
	/**
	 * Gets the string representing reference currency registered on preferences to display chart reports.
	 * @param context The activity context
	 * @return  The string representing the currency registered as a reference to display chart reports or null if not configured yet.
	 */
	public static String getReferenceCurrencyTitle(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString("report_reference_currency", "");
	}
	
	/**
	 * Gets the reference currency registered on preferences to display chart reports.
	 * @param context The activity context
	 * @return The currency registered as a reference to display chart reports or null if not configured yet.
	 */
	public static Currency getReferenceCurrency(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		List<Currency> currencies = CurrencyCache.getAllCurrencies();
		Currency cur = null;
		try { 
			String refCurrency = sharedPreferences.getString("report_reference_currency", null);
			if (currencies!=null && currencies.size()>0) {
				for (Currency currency : currencies) {
					if (currency.title.equals(refCurrency)) cur = currency; 
				}
			}
		} catch (Exception e) {
			return null;
		}
		return cur;
	}

	/**
	 * Gets the period of reference (number of Months to display the 2D report) registered on preferences.
	 * @param context The activity context
	 * @return The number of months registered as a period of reference to display chart reports or 0 if not configured yet.
	 */
	public static int getPeriodOfReference(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String p = sharedPreferences.getString("report_reference_period", "0");
		return Integer.parseInt(p);
	}
	
	/**
	 * Gets the reference month.
	 * @param context The activity context.
	 * @return The reference month that represents the end of the report period.
	 */
	public static int getReferenceMonth(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String month = sharedPreferences.getString("report_reference_month", "0");
		return Integer.parseInt(month);
	}
	
	/**
	 * Gets the flag that indicates if the sub categories will be available individually in 2D report or not.
	 * @param context The activity context.
	 * @return True if the sub categories shall be displayed in the Report 2D list of categories, false otherwise. 
	 */
	public static boolean includeSubCategoriesInReport(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("report_include_sub_categories", true);
	}
	
	/**
	 * Gets the flag that indicates if the list of filter ids will include No Filter (no category, no project or current location) or not.
	 * @param context The activity context.
	 * @return True if no category, no project and current location shall be displayed in 2D Reports, false otherwise.
	 */
	public static boolean includeNoFilterInReport(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("report_include_no_filter", true);
	}
	
	/**
	 * Get the flag that indicates if the category monthly result will consider the result of its sub categories or not.
	 * @param context The activity context.
	 * @return True if the category result shall include the result of its categories, false otherwise.
	 */
	public static boolean addSubCategoriesToSum(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("report_add_sub_categories_result", false);
	}
	
	/**
	 * Gets the flag that indicates if the statistics calculation will consider null values or not.
	 * @param context The activity context.
	 * @return True if the null values shall impact the statistics, false otherwise.
	 */
	public static boolean considerNullResultsInReport(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("report_consider_null_results", true);
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
	
	public static boolean isRestoreMissedScheduledTransactions(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("restore_missed_scheduled_transactions", true);
	}		

    public static boolean isShowRunningBalance(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("show_running_balance", true);
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

    public static boolean isCameraSupported(Context context) {
        return isFeatureSupported(context, PackageManager.FEATURE_CAMERA);
    }

    public static boolean isLocationSupported(Context context) {
        return isFeatureSupported(context, PackageManager.FEATURE_LOCATION);
    }

    public static boolean isLocationNetworkSupported(Context context) {
        return isFeatureSupported(context, PackageManager.FEATURE_LOCATION_NETWORK);
    }

    public static boolean isLocationGPSSupported(Context context) {
        return isFeatureSupported(context, PackageManager.FEATURE_LOCATION_GPS);
    }

    public static boolean isAutoBackupEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("auto_backup_enabled", false);
    }
    
    public static int getAutoBackupTime(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("auto_backup_time", 600);
    }

    private static boolean isFeatureSupported(Context context, String feature) {
        if (hasSystemFeatureMethod != null) {
            PackageManager pm = context.getPackageManager();
            try {
                return (Boolean)hasSystemFeatureMethod.invoke(pm, feature);
            } catch (Exception e) {
                Log.w("Financisto", "Some problems executing PackageManager.hasSystemFeature("+feature+")", e);
                return false;
            }
        }
        Log.i("Financisto", "It's an old device - no PackageManager.hasSystemFeature");
        return true;
    }

    public static boolean shouldRebuildRunningBalance(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean result = sharedPreferences.getBoolean("should_rebuild_running_balance", true);
        if (result) {
            sharedPreferences.edit().putBoolean("should_rebuild_running_balance", false).commit();
        }
        return result;
    }

    public static String getDatabaseBackupFolder(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("database_backup_folder", Export.DEFAULT_EXPORT_PATH.getAbsolutePath());
    }

    public static void setDatabaseBackupFolder(Context context, String databaseBackupFolder) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString("database_backup_folder", databaseBackupFolder).commit();
    }

	public static String[] getReportPreferences(Context context) {
		String[] preferences = new String[7];
		preferences[0] = getReferenceCurrencyTitle(context);
		preferences[1] = Integer.toString(getPeriodOfReference(context));
		preferences[2] = Integer.toString(getReferenceMonth(context));
		preferences[3] = Boolean.toString(considerNullResultsInReport(context));
		preferences[4] = Boolean.toString(includeNoFilterInReport(context));
		preferences[5] = Boolean.toString(includeSubCategoriesInReport(context));
		preferences[6] = Boolean.toString(addSubCategoriesToSum(context));
		return preferences;
	}

    public static boolean isQuickMenuEnabledForAccount(Context context) {
        return getBoolean(context, "quick_menu_account_enabled", true) && isSupportedApiLevel();
    }

    public static boolean isQuickMenuEnabledForTransaction(Context context) {
        return getBoolean(context, "quick_menu_transaction_enabled", true) && isSupportedApiLevel();
    }

    private static boolean getBoolean(Context context, String name, boolean defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(name, defaultValue);
    }

}
