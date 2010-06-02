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
package ru.orangesoftware.financisto.blotter;

import java.util.ArrayList;
import java.util.List;

import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class BlotterTotalsCalculationTask extends AsyncTask<Object, Total, Total[]> {
	
	public static final String[] BALANCE_PROJECTION = {
		"from_account_currency_id",
		"SUM(from_amount)"};

	public static final String BALANCE_GROUPBY = "from_account_currency_id";

	private volatile boolean isRunning = true;
	
	private final Context context;
	private final DatabaseAdapter db;
	private final WhereFilter blotterFilter;
	private final ViewFlipper totalTextFlipper;
	private final TextView totalText;
	private final boolean filterAccounts;
	
	public BlotterTotalsCalculationTask(Context context, DatabaseAdapter db, 
			WhereFilter blotterFilter, ViewFlipper totalTextFlipper, TextView totalText,
			boolean filterAccounts) {
		this.context = context;
		this.db = db;
		this.blotterFilter = blotterFilter;
		this.totalTextFlipper = totalTextFlipper;
		this.totalText = totalText;
		this.filterAccounts = filterAccounts;
	}

	private Total[] getTransactionsBalance(WhereFilter filter) {
		if (filterAccounts) {
			filter = WhereFilter.copyOf(filter);
			filter.put(Criteria.eq("from_account_is_include_into_totals", "1"));			
		}
		Cursor c = db.db().query("v_blotter_for_account", BALANCE_PROJECTION, 
				filter.getSelection(), filter.getSelectionArgs(), 
				BALANCE_GROUPBY, null, null);
		//DatabaseUtils.dumpCursor(c);
		try {			
			int count = c.getCount();
			List<Total> totals = new ArrayList<Total>(count);
			while (c.moveToNext()) {
				long currencyId = c.getLong(0);
				long balance = c.getLong(1);
				Currency currency = CurrencyCache.getCurrency(currencyId);
				Total total = new Total(currency);
				total.balance = balance; 
				totals.add(total);
			}
			return totals.toArray(new Total[totals.size()]);
		} finally {
			c.close();
		}
	}

	@Override
	protected Total[] doInBackground(Object... params) {
		try {
			return getTransactionsBalance(blotterFilter);
		} catch (Exception ex) {
			Log.e("TotalBalance", "Unexpected error", ex);
			return new Total[0];
		}
	}

	@Override
	protected void onPostExecute(Total[] result) {
		if (isRunning) {
			setTotals(context, totalTextFlipper, totalText, result);
		}
	}
	
	public void stop() {
		isRunning = false;
	}
	
	public static void setTotals(Context context, ViewFlipper totalTextFlipper, TextView totalText, Total[] totals) {
		if (totalTextFlipper.isFlipping()) {
			totalTextFlipper.removeAllViews();
		} else {
			totalTextFlipper.setFlipInterval(2000);
			totalTextFlipper.setInAnimation(context, android.R.anim.fade_in);
			totalTextFlipper.setOutAnimation(context, android.R.anim.fade_out);
			totalTextFlipper.setAnimateFirstView(false);
			totalTextFlipper.startFlipping();
		}
		Utils u = new Utils(context);
		if (totals.length > 1) {
			for (Total t : totals) {
				TextView tv = createTextView(context);
				if (t.showAmount) {
					Utils.setTotal(context, tv, t);
				} else {
					u.setAmountText(tv, t.currency, t.balance, false);
				}
				totalTextFlipper.addView(tv);
				totalText.setVisibility(View.GONE);
				totalTextFlipper.setVisibility(View.VISIBLE);
			}			
		} else {
			if (totals.length > 0) {
				Total t = totals[0];
				if (t.showAmount) {
					Utils.setTotal(context, totalText, t);
				} else {
					u.setAmountText(totalText, t.currency, t.balance, false);
				}				
			} else {
				u.setAmountText(totalText, Currency.EMPTY, 0, false);
			}
			totalTextFlipper.setVisibility(View.GONE);
			totalText.setVisibility(View.VISIBLE);			
		}		
	}

	private static TextView createTextView(Context context) {
		TextView tv = new TextView(context);
		tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		tv.setTypeface(Typeface.DEFAULT_BOLD);
		return tv;
	}
	
}
