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

import java.util.HashMap;

import ru.orangesoftware.financisto.adapter.TransactionsListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterTotalsCalculationTask;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class BudgetBlotterActivity extends BlotterActivity {
	
	private static final String[] SUM_FROM_AMOUNT = new String[]{"sum(from_amount)"};
	
	private BudgetCalculationTask calculationTask;
	private HashMap<Long, Category> categories;
	private HashMap<Long, Project> projects;
	
    public BudgetBlotterActivity() {
		super();
	}
        
	@Override
	protected void calculateTotals() {	
		if (calculationTask != null) {
			calculationTask.stop();
			calculationTask.cancel(true);
		}
		calculationTask = new BudgetCalculationTask(totalTextFlipper, totalText, blotterFilter.getBudgetId()); 
		calculationTask.execute();
	}
	
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		categories = MyEntity.asMap(db.getAllCategoriesList(true));
		projects = MyEntity.asMap(em.getAllProjectsList(true));
		super.internalOnCreate(savedInstanceState);
		bFilter.setVisibility(View.GONE);
	}
	
	@Override
	protected Cursor createCursor() {
		long budgetId = blotterFilter.getBudgetId();
		return getBlotterForBudget(budgetId);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new TransactionsListAdapter(this, cursor);
	}
	
	private Cursor getBlotterForBudget(long budgetId) {
		Budget b = em.load(Budget.class, budgetId);
		String where = Budget.createWhere(b, categories, projects);
		return db.getBlotter(where);
	}
	
	public class BudgetCalculationTask extends AsyncTask<Void, Total, Total[]> {
		
		private volatile boolean isRunning = true;
		
		private final ViewFlipper totalTextFlipper;
		private final TextView totalText;
		private final long budgetId;
		
		public BudgetCalculationTask(ViewFlipper totalTextFlipper, TextView totalText, long budgetId) {
			this.totalTextFlipper = totalTextFlipper;
			this.totalText = totalText;
			this.budgetId = budgetId;
		}

		@Override
		protected Total[] doInBackground(Void... params) {
			long t0 = System.currentTimeMillis();
			try {
				try {
					Budget b = em.load(Budget.class, budgetId);
					Currency c = CurrencyCache.getCurrency(b.currencyId);
					Total[] totals = new Total[]{new Total(c)};
					totals[0].balance = queryBalanceSpend(categories, projects, b);
					return totals;
				} finally {
					long t1 = System.currentTimeMillis();
					Log.d("BUDGET TOTALS", (t1-t0)+"ms");
				}
			} catch (Exception ex) {
				Log.e("BudgetTotals", "Unexpected error", ex);
				return new Total[0];
			}
		}

		@Override
		protected void onPostExecute(Total[] result) {
			if (isRunning) {
				BlotterTotalsCalculationTask.setTotals(BudgetBlotterActivity.this, totalTextFlipper, totalText, result);
				((TransactionsListAdapter)adapter).notifyDataSetChanged();
			}
		}
		
		public void stop() {
			isRunning = false;
		}
		
	}
	
	public long queryBalanceSpend(HashMap<Long, Category> categories, HashMap<Long, Project> projects, Budget b) {
		String where = Budget.createWhere(b, categories, projects);
		Log.d("BUDGETS", where);
		Cursor c = db.db().query("v_blotter_for_account", SUM_FROM_AMOUNT, where, null, null, null, null);
		try {
			if (c.moveToNext()) {
				return c.getLong(0);
			}
		} finally {
			c.close();
		}
		return 0;
	}

}
