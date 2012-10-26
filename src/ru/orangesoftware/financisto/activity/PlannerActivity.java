/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ScheduledListAdapter;
import ru.orangesoftware.financisto.utils.FuturePlanner;
import ru.orangesoftware.financisto.utils.TransactionList;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 10/20/12 2:22 PM
 */
public class PlannerActivity extends AbstractListActivity {

    public PlannerActivity() {
        super(R.layout.planner);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
    }

    @Override
    protected Cursor createCursor() {
        retrieveData();
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return null;
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
    }

    @Override
    protected void editItem(View v, int position, long id) {
    }

    @Override
    protected void viewItem(View v, int position, long id) {
    }

    private PlannerTask task;

    private void retrieveData() {
        if (task != null) {
            task.cancel(true);
        }
        task = new PlannerTask();
        task.execute();
    }

    private class PlannerTask extends AsyncTask<Void, Void, TransactionList> {

        @Override
        protected TransactionList doInBackground(Void... voids) {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.MONTH, 1);
            FuturePlanner planner = new FuturePlanner(db, date.getTime(), new Date());
            return planner.getPlannedTransactionsWithTotals();
        }

        @Override
        protected void onPostExecute(TransactionList data) {
            ScheduledListAdapter adapter = new ScheduledListAdapter(PlannerActivity.this, data.transactions);
            setListAdapter(adapter);
        }

    }

}
