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
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ReportAdapter;
import ru.orangesoftware.financisto.blotter.TotalCalculationTask;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.report.PeriodReport;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportData;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.Utils;

public class ReportActivity extends ListActivity implements RecreateCursorSupportedActivity {

	private DatabaseAdapter db;
	private ImageButton bFilter;
	private Report currentReport;
    private ReportAsyncTask reportTask;
	
	private WhereFilter filter = WhereFilter.empty();
    private boolean saveFilter = false;
	
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
                saveFilter = true;
            }
			currentReport = ReportsListActivity.createReport(this, db.em(), intent.getExtras());
			selectReport();
		}

        applyFilter();
        showOrRemoveTotals();
	}

	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
	}
	
    private void showOrRemoveTotals() {
        if (!currentReport.shouldDisplayTotal()) {
            findViewById(R.id.labelTotal).setVisibility(View.GONE);
        }
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
	public void recreateCursor() {
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
        if (saveFilter) {
            SharedPreferences preferences = getPreferences(0);
            filter.toSharedPreferences(preferences);
        }
        applyFilter();
    }
	
    private class ReportAsyncTask extends AsyncTask<Void, Void, ReportData> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
            ((TextView)findViewById(android.R.id.empty)).setText(R.string.calculating);
        }

        @Override
        protected ReportData doInBackground(Void...voids) {
            return currentReport.getReport(db, WhereFilter.copyOf(filter));
        }

        @Override
        protected void onPostExecute(ReportData data) {
            setProgressBarIndeterminateVisibility(false);
            displayTotal(data.total);
            ((TextView) findViewById(android.R.id.empty)).setText(R.string.empty_report);
            ReportAdapter adapter = new ReportAdapter(ReportActivity.this, data.units);
            setListAdapter(adapter);
        }

    }

    private void displayTotal(Total total) {
        if (currentReport.shouldDisplayTotal()) {
            TextView totalText = (TextView)findViewById(R.id.total);
            Utils u = new Utils(this);
            u.setTotal(totalText, total);
        }
    }

}
