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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.CardIssuer;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.orb.EntityManager;

import java.util.Arrays;

import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

public class AccountWidget extends AppWidgetProvider {

    private static final String WIDGET_UPDATE_ACTION = "ru.orangesoftware.financisto.UPDATE_WIDGET";

    private static final String PREFS_NAME = "ru.orangesoftware.financisto.activity.AccountWidget";
    private static final String PREF_PREFIX_KEY = "prefix_";

    public static final String WIDGET_ID = "widgetId";

    public static void updateWidgets(Context context) {
        ComponentName thisWidget = new ComponentName(context, AccountWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(thisWidget);
        updateWidgets(context, manager, widgetIds, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("FinancistoWidget", "onReceive intent "+intent);
        String action = intent.getAction();
        if (WIDGET_UPDATE_ACTION.equals(action)) {
            int widgetId = intent.getIntExtra(WIDGET_ID, INVALID_APPWIDGET_ID);
            if (widgetId != INVALID_APPWIDGET_ID) {
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                updateWidgets(context, manager, new int[]{widgetId}, true);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        updateWidgets(context, manager, appWidgetIds, false);
    }

    private static void updateWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds, boolean nextAccount) {
        Log.d("FinancistoWidget", "updateWidgets "+ Arrays.toString(appWidgetIds)+" -> "+nextAccount);
        if (MyPreferences.isWidgetEnabled(context)) {
            for (int id : appWidgetIds) {
                long accountId = loadAccountForWidget(context, id);
                RemoteViews remoteViews = nextAccount || accountId == -1
                        ? buildUpdateForNextAccount(context, id, accountId)
                        : buildUpdateForCurrentAccount(context, id, accountId);
                manager.updateAppWidget(id, remoteViews);
            }
        } else {
            manager.updateAppWidget(appWidgetIds, noDataUpdate(context));
        }
    }


    private static long loadAccountForWidget(Context context, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getLong(PREF_PREFIX_KEY + widgetId, -1);
    }

    private static void saveAccountForWidget(Context context, int widgetId, long accountId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_PREFIX_KEY + widgetId, accountId);
        prefs.commit();
    }

    private static RemoteViews updateWidgetFromAccount(Context context, int widgetId, Account a) {
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_2x1);
        updateViews.setTextViewText(R.id.line1, a.title);
        AccountType type = AccountType.valueOf(a.type);
        if (type.isCard && a.cardIssuer != null) {
            CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
            updateViews.setImageViewResource(R.id.account_icon, cardIssuer.iconId);
        } else {
            updateViews.setImageViewResource(R.id.account_icon, type.iconId);
        }
        long amount = a.totalAmount;
        updateViews.setTextViewText(R.id.note, Utils.amountToString(a.currency, amount));
        Utils u = new Utils(context);
        int amountColor = u.getAmountColor(context, amount);
        updateViews.setTextColor(R.id.note, amountColor);
        addScrollOnClick(context, updateViews, widgetId);
        addTapOnClick(context, updateViews);
        saveAccountForWidget(context, widgetId, a.id);
        return updateViews;
    }

    private static void addScrollOnClick(Context context, RemoteViews updateViews, int widgetId) {
        Intent intent = new Intent(context, AccountWidget.class);
        intent.setAction(WIDGET_UPDATE_ACTION);
        intent.putExtra(WIDGET_ID, widgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.account_icon, pendingIntent);
    }

    private static void addTapOnClick(Context context, RemoteViews updateViews) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        updateViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
    }

    private static RemoteViews buildUpdateForCurrentAccount(Context context, int widgetId, long accountId) {
        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();
        try {
            MyEntityManager em = db.em();
            Account a = em.getAccount(accountId);
            if (a != null) {
                Log.d("FinancistoWidget", "buildUpdateForCurrentAccount building update for "+widgetId+" -> "+accountId);
                return updateWidgetFromAccount(context, widgetId, a);
            } else {
                Log.d("FinancistoWidget", "buildUpdateForCurrentAccount not found "+widgetId+" -> "+accountId);
                return buildUpdateForNextAccount(context, widgetId, -1);
            }
        } catch (Exception ex) {
            return errorUpdate(context);
        } finally {
            db.close();
        }
    }

    private static RemoteViews buildUpdateForNextAccount(Context context, int widgetId, long accountId) {
        DatabaseAdapter db = new DatabaseAdapter(context);
        db.open();
        try {
            MyEntityManager em = db.em();
            Cursor c = em.getAllActiveAccounts();
            try {
                int count = c.getCount();
                if (count > 0) {
                    Log.d("FinancistoWidget", "buildUpdateForNextAccount "+widgetId+" -> "+accountId);
                    if (count == 1 || accountId == -1) {
                        if (c.moveToNext()) {
                            Account a = EntityManager.loadFromCursor(c, Account.class);
                            return updateWidgetFromAccount(context, widgetId, a);
                        }
                    } else {
                        boolean found = false;
                        while (c.moveToNext()) {
                            Account a = EntityManager.loadFromCursor(c, Account.class);
                            if (a.id == accountId) {
                                found = true;
                                Log.d("FinancistoWidget", "buildUpdateForNextAccount found -> "+accountId);
                            } else {
                                if (found) {
                                    Log.d("FinancistoWidget", "buildUpdateForNextAccount building update for -> "+a.id);
                                    return updateWidgetFromAccount(context, widgetId, a);
                                }
                            }
                        }
                        c.moveToFirst();
                        Account a = EntityManager.loadFromCursor(c, Account.class);
                        Log.d("FinancistoWidget", "buildUpdateForNextAccount not found, taking the first one -> "+a.id);
                        return updateWidgetFromAccount(context, widgetId, a);
                    }
                }
                return noDataUpdate(context);
            } finally {
                c.close();
            }
        } catch (Exception ex) {
            return errorUpdate(context);
        } finally {
            db.close();
        }
    }

    private static RemoteViews errorUpdate(Context context) {
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_2x1_no_data);
        updateViews.setTextViewText(R.id.line1, "Error!");
        updateViews.setTextColor(R.id.line1, Color.RED);
        return updateViews;
    }

    private static RemoteViews noDataUpdate(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_2x1_no_data);
    }

}
