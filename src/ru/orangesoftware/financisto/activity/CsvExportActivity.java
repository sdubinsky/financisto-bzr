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

import java.text.DateFormat;
import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.DateUtils.Period;
import ru.orangesoftware.financisto.utils.DateUtils.PeriodType;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

public class CsvExportActivity extends Activity {
	
	public static final String CSV_EXPORT_DECIMALS = "CSV_EXPORT_DECIMALS";
	public static final String CSV_EXPORT_DECIMAL_SEPARATOR = "CSV_EXPORT_DECIMAL_SEPARATOR";
	public static final String CSV_EXPORT_GROUP_SEPARATOR = "CSV_EXPORT_GROUP_SEPARATOR";
	public static final String CSV_EXPORT_DATE_FORMAT = "CSV_EXPORT_DATE_FORMAT";
	public static final String CSV_EXPORT_TIME_FORMAT = "CSV_EXPORT_TIME_FORMAT";
	public static final String CSV_EXPORT_FIELD_SEPARATOR = "CSV_EXPORT_FIELD_SEPARATOR";
	public static final String CSV_EXPORT_INCLUDE_HEADER = "CSV_EXPORT_INCLUDE_HEADER";

	private final WhereFilter filter = WhereFilter.empty();

	private Button bPeriod;
	private DateFormat df;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.csv_export);
		
		df = DateUtils.getShortDateFormat(this);
		
		final Spinner groupSeparators = (Spinner)findViewById(R.id.spinnerGroupSeparators);
		groupSeparators.setSelection(1);
		filter.put(new DateTimeCriteria(PeriodType.THIS_MONTH));
		
		bPeriod = (Button)findViewById(R.id.bPeriod);
		bPeriod.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(CsvExportActivity.this, DateFilterActivity.class);
				filter.toIntent(intent);
				startActivityForResult(intent, 1);
			}
		});

		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				Spinner decimals = (Spinner)findViewById(R.id.spinnerDecimals);
				Spinner decimalSeparators = (Spinner)findViewById(R.id.spinnerDecimalSeparators);
				Spinner fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
				CheckBox includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);

				Intent data = new Intent();
				filter.toIntent(data);
				
				data.putExtra(CSV_EXPORT_DECIMALS, 2-decimals.getSelectedItemPosition());
				data.putExtra(CSV_EXPORT_DECIMAL_SEPARATOR, decimalSeparators.getSelectedItem().toString());
				data.putExtra(CSV_EXPORT_GROUP_SEPARATOR, groupSeparators.getSelectedItem().toString());
				data.putExtra(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItem().toString().charAt(1));
				data.putExtra(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
				
				setResult(RESULT_OK, data);
				finish();
			}		
		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}		
		});
		
		updatePeriod();
	}

	private void updatePeriod() {
		DateTimeCriteria c = filter.getDateTime();
		if (c == null) {
			bPeriod.setText(R.string.no_filter);
		} else {
			Period p = c.getPeriod();
			if (p.isCustom()) {
				long periodFrom = c.getLongValue1();
				long periodTo = c.getLongValue2();
				bPeriod.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
			} else {
				bPeriod.setText(p.type.titleId);
			}		
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_FIRST_USER) {
				filter.clearDateTime();
			} else if (resultCode == RESULT_OK) {
				String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
				PeriodType p = PeriodType.valueOf(periodType);
				if (PeriodType.CUSTOM == p) {
					long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
					long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
					filter.put(new DateTimeCriteria(periodFrom, periodTo));
				} else {
					filter.put(new DateTimeCriteria(p));
				}			
			}
			updatePeriod();
		}
	}


	
}
