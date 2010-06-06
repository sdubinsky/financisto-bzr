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

import java.util.ArrayList;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ScheduledListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.recur.RecurrenceScheduler;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ScheduledListActivity extends BlotterActivity {

	public ScheduledListActivity() {}
	
	public ScheduledListActivity(int layoutId) {
		super(layoutId);
	}
	
	@Override
	protected void calculateTotals() {
		// do nothing
	}
	
	@Override
	protected Cursor createCursor() {
		return null;
	}
	
	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		ArrayList<TransactionInfo> transactions = RecurrenceScheduler.getSortedSchedules(em);
		return new ScheduledListAdapter(this, transactions);
	}

	@Override
	public void requeryCursor() {
		ArrayList<TransactionInfo> transactions = RecurrenceScheduler.getSortedSchedules(em);
		updateAdapter(transactions);		
		RecurrenceScheduler.scheduleAll(this, transactions);
	}

	private void updateAdapter(ArrayList<TransactionInfo> transactions) {
		((ScheduledListAdapter)adapter).setTransactions(transactions);
	}
	
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);
		// remove filter button and totals
		bFilter.setVisibility(View.GONE);
		bTemplate.setVisibility(View.GONE);
		findViewById(R.id.totalLayout).setVisibility(View.GONE);
		internalOnCreateTemplates();
	}

	protected void internalOnCreateTemplates() {
		// change empty list message
		((TextView)findViewById(android.R.id.empty)).setText(R.string.no_scheduled_transactions);
		// fix filter
		blotterFilter = new WhereFilter("schedules");
		blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			requeryCursor();
		}
	}

}
