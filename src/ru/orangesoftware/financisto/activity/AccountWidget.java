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
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.CardIssuer;
import ru.orangesoftware.financisto.service.FinancistoService;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.orb.EntityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.widget.RemoteViews;

public class AccountWidget extends AppWidgetProvider {
	
	public static final String WIDGET_ACCOUNT_ID = "widgetAccountId";
	public static final String TRANSACTION_ACCOUNT_ID = "transactionAccountId";
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	FinancistoService.updateWidget(context);
    }

    public static RemoteViews buildUpdatesForAccount(Context context, long accountId) {
    	DatabaseAdapter db = new DatabaseAdapter(context);
		db.open();
		try {
			MyEntityManager em = new MyEntityManager(context, db.db());
			Account a = em.getAccount(accountId);
			return updateFromAccount(context, a);
		} catch (Exception ex) { 
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_2x1_no_data);
			updateViews.setTextViewText(R.id.line1, "Errror!");
			updateViews.setTextColor(R.id.line1, Color.RED);
			return updateViews;
		} finally {
			db.close();
		}
	}

    private static RemoteViews updateFromAccount(Context context, Account a) {
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
		addScrollOnClick(context, updateViews, a);
		addTapOnClick(context, updateViews);
        return updateViews;
	}

	private static void addScrollOnClick(Context context, RemoteViews updateViews, Account a) {
		Intent intent = new Intent(context, FinancistoService.class);
		intent.setAction(FinancistoService.WIDGET_UPDATE_ACTION);
		intent.putExtra(WIDGET_ACCOUNT_ID, a.id);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        updateViews.setOnClickPendingIntent(R.id.account_icon, pendingIntent);
	}

	private static void addTapOnClick(Context context, RemoteViews updateViews) {
		Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        updateViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
	}

	public static RemoteViews buildUpdate(Context context, long accountId) {        	        
    	DatabaseAdapter db = new DatabaseAdapter(context);
		db.open();
		try {
			MyEntityManager em = new MyEntityManager(context, db.db());
			Cursor c = em.getAllActiveAccounts();
			try {
				int count = c.getCount();
				if (count > 0) {
					if (count == 1 || accountId == -1) {
						if (c.moveToNext()) {
							Account a = EntityManager.loadFromCursor(c, Account.class);
							return updateFromAccount(context, a);
						}
					} else {
		    			boolean found = false;
		    			while (c.moveToNext()) {
		    				Account a = EntityManager.loadFromCursor(c, Account.class);
		    				if (a.id == accountId) {
		    					found = true;
		    				} else {
		    					if (found) {
		    						return updateFromAccount(context, a);
		    					}
		    				}
		    			}
		    			c.moveToFirst();
	    				Account a = EntityManager.loadFromCursor(c, Account.class);
						return updateFromAccount(context, a);
					}
				}
			} finally {
				c.close();
			}
		} catch (Exception ex) { 
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_2x1_no_data);
			updateViews.setTextViewText(R.id.line1, "Errror!");
			updateViews.setTextColor(R.id.line1, Color.RED);
			return updateViews;
		} finally {
			db.close();
		}
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_2x1_no_data);
		return updateViews;
    }


}
