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

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.*;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ReportAdapter;
import ru.orangesoftware.financisto.blotter.BlotterTotalsCalculationTask;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.graph.Amount;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.report.AbstractReport;
import ru.orangesoftware.financisto.report.PeriodReport;
import ru.orangesoftware.financisto.report.Report;

import java.util.ArrayList;
import java.util.HashMap;

public class ReportActivity extends ListActivity implements RequeryCursorActivity {
	
	private DatabaseAdapter db;
	private ImageButton bFilter;
	private Report currentReport;
    private ReportAsyncTask reportTask;
	
	private WhereFilter filter = WhereFilter.empty();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.report);

        applyAnimationToListView();

        db = new DatabaseAdapter(this);
		db.open();
		
		bFilter = (ImageButton)findViewById(R.id.bFilter);
		bFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ReportActivity.this, DateFilterActivity.class);
				filter.toIntent(intent);
				startActivityForResult(intent, 1);
			}
		});
				
		Intent intent = getIntent();
		if (intent != null) {
			filter = WhereFilter.fromIntent(intent);
            if (filter.isEmpty()) {
                filter = WhereFilter.fromSharedPreferences(getPreferences(0));
            }
			currentReport = ReportsListActivity.createReport(this, intent.getExtras());
			selectReport();
		}

        applyFilter();
	}

    private void applyAnimationToListView() {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(50);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(100);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
        ListView listView = getListView();
        listView.setLayoutAnimation(controller);
    }

    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (currentReport != null) {
			Intent intent = currentReport.createActivityIntent(this, db, WhereFilter.copyOf(filter), id);
			startActivity(intent);
		}
	}

	private void selectReport() {
        cancelCurrentReportTask();
        reportTask = new ReportAsyncTask();
        reportTask.execute();
	}

    private void cancelCurrentReportTask() {
        if (reportTask != null) {
            reportTask.cancel(true);
        }
    }

    private void applyFilter() {
        TextView tv = (TextView)findViewById(R.id.period);
        if (currentReport instanceof PeriodReport) {
            disableFilter();
            tv.setVisibility(View.GONE);
        } else {
            Criteria c = filter.get(ReportColumns.DATETIME);
            if (c != null) {
                tv.setText(DateUtils.formatDateRange(this, c.getLongValue1(), c.getLongValue2(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
            } else {
                tv.setText(R.string.no_filter);
            }
            enableFilter();
            tv.setVisibility(View.VISIBLE);
        }
    }

    protected void disableFilter() {
        bFilter.setEnabled(false);
        bFilter.setImageResource(R.drawable.ic_menu_filter_off);
    }

    protected void enableFilter() {
        bFilter.setEnabled(true);
        bFilter.setImageResource(filter.isEmpty() ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
    }

    @Override
	protected void onDestroy() {
        cancelCurrentReportTask();
		db.close();
		super.onDestroy();
	}

	@Override
	public void requeryCursor() {
		selectReport();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_FIRST_USER) {
				filter.clearDateTime();
                saveFilter();
				selectReport();
			} else if (resultCode == RESULT_OK) {
				DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
				filter.put(c);
                saveFilter();
				selectReport();
			}
		}
	}

    private void saveFilter() {
        SharedPreferences preferences = getPreferences(0);
        filter.toSharedPreferences(preferences);
        applyFilter();
    }
	
    private class ReportAsyncTask extends AsyncTask<Void, Void, ArrayList<GraphUnit>> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
            ((TextView)findViewById(android.R.id.empty)).setText(R.string.calculating);
        }

        @Override
        protected ArrayList<GraphUnit> doInBackground(Void...voids) {
            return currentReport.getReport(db, WhereFilter.copyOf(filter));
        }

        @Override
        protected void onPostExecute(ArrayList<GraphUnit> units) {
            setProgressBarIndeterminateVisibility(false);
            displayTotal(units);
            ((TextView) findViewById(android.R.id.empty)).setText(R.string.empty_report);
            ReportAdapter adapter = new ReportAdapter(ReportActivity.this, units);
            setListAdapter(adapter);
        }

    }

    private void displayTotal(ArrayList<GraphUnit> units) {
        Total[] totals = calculateTotals(units);
        ViewFlipper totalTextFlipper = (ViewFlipper)findViewById(R.id.flipperTotal);
        TextView totalText = (TextView)findViewById(R.id.total);
        BlotterTotalsCalculationTask.setTotals(this, totalTextFlipper, totalText, totals);
    }

    private Total[] calculateTotals(ArrayList<GraphUnit> units) {
        HashMap<Long, Total> map = new HashMap<Long, Total>();
        for (GraphUnit u : units) {
            for (Amount a : u.amounts.values()) {
                Total t = getOrCreate(map, a.currency);
                long amount = a.amount;
                if (amount > 0) {
                    t.amount += amount;
                } else {
                    t.balance += amount;
                }
            }
        }
        return map.values().toArray(new Total[map.size()]);
    }

    private Total getOrCreate(HashMap<Long, Total> map, Currency currency) {
        Total t = map.get(currency.id);
        if (t == null) {
            t = new Total(currency, true);
            map.put(currency.id, t);
        }
        return t;
    }

}
