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
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.adapter.ReportListAdapter;
import ru.orangesoftware.financisto.report.Report;
import ru.orangesoftware.financisto.report.ReportType;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class ReportsListActivity extends ListActivity {
	
	public static final String EXTRA_REPORT_TYPE = "reportType";
	
	public final ReportType[] reports = new ReportType[]{
			ReportType.BY_PERIOD,
			ReportType.BY_CATEGORY,
			ReportType.BY_LOCATION,
			ReportType.BY_PROJECT			
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reports_list);		
		setListAdapter(new ReportListAdapter(this, reports));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, ReportActivity.class);
		intent.putExtra(EXTRA_REPORT_TYPE, reports[position].name());
		startActivity(intent);
	}

	public static Report createReport(Context context, Bundle extras) {
		String reportTypeName = extras.getString(EXTRA_REPORT_TYPE);
		ReportType reportType = ReportType.valueOf(reportTypeName);
		return reportType.createReport(context, extras);
	}

}
