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

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.TransactionsTotalCalculator;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.Utils;

public class BlotterTotalsCalculationTask extends AsyncTask<Object, Total, Total[]> {
	
	private volatile boolean isRunning = true;
	
	private final Context context;
	private final DatabaseAdapter db;
	private final WhereFilter filter;
	private final ViewFlipper totalTextFlipper;
	private final TextView totalText;

	public BlotterTotalsCalculationTask(Context context, DatabaseAdapter db, 
			WhereFilter filter, ViewFlipper totalTextFlipper, TextView totalText) {
		this.context = context;
		this.db = db;
		this.filter = filter;
		this.totalTextFlipper = totalTextFlipper;
		this.totalText = totalText;
	}

	protected Total[] getTransactionsBalance() {
        TransactionsTotalCalculator calculator = createTotalCalculator();
        return calculator.getTransactionsBalance();
	}

    protected TransactionsTotalCalculator createTotalCalculator() {
        return new TransactionsTotalCalculator(db, filter);
    }

    @Override
	protected Total[] doInBackground(Object... params) {
		try {
			return getTransactionsBalance();
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
