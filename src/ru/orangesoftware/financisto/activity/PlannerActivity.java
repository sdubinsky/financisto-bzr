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
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ScheduledListAdapter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.FuturePlanner;
import ru.orangesoftware.financisto.utils.TransactionList;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 10/20/12 2:22 PM
 */
public class PlannerActivity extends AbstractListActivity {

    private TextView totalText;
    private ImageButton bFilter;
    private WhereFilter filter = WhereFilter.empty();

    public PlannerActivity() {
        super(R.layout.planner);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        totalText = (TextView)findViewById(R.id.total);
        bFilter = (ImageButton)findViewById(R.id.bFilter);
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
        task = new PlannerTask(filter);
        task.execute();
    }

    private class PlannerTask extends AsyncTask<Void, Void, TransactionList> {

        private final WhereFilter filter;

        private PlannerTask(WhereFilter filter) {
            this.filter = WhereFilter.copyOf(filter);
        }

        @Override
        protected TransactionList doInBackground(Void... voids) {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.MONTH, 1);
            Date now = new Date();
            filter.put(new DateTimeCriteria(now.getTime(), date.getTimeInMillis()));
            FuturePlanner planner = new FuturePlanner(db, filter, now);
            return planner.getPlannedTransactionsWithTotals();
        }

        @Override
        protected void onPostExecute(TransactionList data) {
            ScheduledListAdapter adapter = new ScheduledListAdapter(PlannerActivity.this, data.transactions);
            setListAdapter(adapter);
            setTotals(data.totals);
        }

    }

    private void setTotals(Total[] totals) {
        Utils u = new Utils(this);
        u.setTotal(totalText, totals[0]);
    }

}
