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
import ru.orangesoftware.financisto.R.id;
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.adapter.ReportAdapter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.report.PeriodReport;
import ru.orangesoftware.financisto.report.Report;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class ReportActivity extends ListActivity implements RequeryCursorActivity {
	
	private DatabaseAdapter db;
	private ImageButton bFilter;
	private Report currentReport;
	
	private WhereFilter filter = WhereFilter.empty();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.report);
		
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
			currentReport = ReportsListActivity.createReport(this, intent.getExtras());
			selectReport();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (currentReport != null) {
			Intent intent = currentReport.createActivityIntent(this, db, filter, id);
			startActivity(intent);
		}
	}

	private void selectReport() {
		ArrayList<GraphUnit> units = currentReport.getReport(db, filter);
		ReportAdapter adapter = new ReportAdapter(this, units);
		if (currentReport instanceof PeriodReport) {
			bFilter.setEnabled(false);
		} else {
			TextView tv = (TextView)findViewById(R.id.period);
			Criteria c = filter.get(ReportColumns.DATETIME);
			if (c != null) {
				tv.setText(DateUtils.formatDateRange(this, c.getLongValue1(), c.getLongValue2(), 
						DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_ABBREV_MONTH));
			} else {
				tv.setText(R.string.no_filter);
			}
			bFilter.setEnabled(true);			
		}
		setListAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
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
				selectReport();
			} else if (resultCode == RESULT_OK) {
				DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
				filter.put(c);
				selectReport();
			}
		}
	}		
	
}
