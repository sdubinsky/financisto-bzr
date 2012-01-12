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
import java.util.Calendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.DateUtils.Period;
import ru.orangesoftware.financisto.utils.DateUtils.PeriodType;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemSelectedListener;

import static ru.orangesoftware.financisto.utils.DateUtils.is24HourFormat;

public class DateFilterActivity extends Activity {
	
	public static final String EXTRA_FILTER_PERIOD_TYPE = "filter_period_type";
	public static final String EXTRA_FILTER_PERIOD_FROM = "filter_period_from";
	public static final String EXTRA_FILTER_PERIOD_TO = "filter_period_to";
	public static final String EXTRA_FILTER_DONT_SHOW_NO_FILTER = "filter_dont_show_no_filter";
	
	private final Calendar cFrom = Calendar.getInstance(); 
	private final Calendar cTo = Calendar.getInstance();
	
	private Spinner spinnerPeriodType;
	private Button buttonPeriodFrom;
	private Button buttonPeriodTo;
	
	private DateFormat df;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.date_filter);

		df = DateUtils.getShortDateFormat(this);
		
		spinnerPeriodType = (Spinner)findViewById(R.id.period);
		spinnerPeriodType.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch(position) {
				case 0:
					selectToday();
					break;
				case 1:
					selectYesterday();
					break;
				case 2:
					selectThisWeek();
					break;
				case 3:
					selectThisMonth();
					break;
				case 4:
					selectLastWeek();
					break;
				case 5:
					selectLastMonth();
					break;
				default:
					selectCustom();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}			
		});		
		
		buttonPeriodFrom = (Button)findViewById(R.id.bPeriodFrom);
		buttonPeriodFrom.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				showDialog(1);
			}
		});		
		buttonPeriodTo = (Button)findViewById(R.id.bPeriodTo);	
		buttonPeriodTo.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				showDialog(2);
			}
		});		
		
		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent data = new Intent();
				PeriodType[] periods = PeriodType.values();
				PeriodType period = periods[spinnerPeriodType.getSelectedItemPosition()];
				data.putExtra(EXTRA_FILTER_PERIOD_TYPE, period.name());
				data.putExtra(EXTRA_FILTER_PERIOD_FROM, cFrom.getTimeInMillis());
				data.putExtra(EXTRA_FILTER_PERIOD_TO, cTo.getTimeInMillis());
				setResult(RESULT_OK, data);
				finish();
			}
		});
		
		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		Button bNoFilter = (Button)findViewById(R.id.bNoFilter);
		bNoFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setResult(RESULT_FIRST_USER);
				finish();
			}
		});		

		Intent intent = getIntent();
		if (intent == null) {
			reset();
		} else {
			WhereFilter filter = WhereFilter.fromIntent(intent);
			DateTimeCriteria c = (DateTimeCriteria)filter.get(BlotterFilter.DATETIME);
			if (c != null) {
				if (c.getPeriod() == null || c.getPeriod().type == PeriodType.CUSTOM) {
					selectPeriod(c.getLongValue1(), c.getLongValue2());					
				} else {
					selectPeriod(c.getPeriod());
				}
				
			}
			if (intent.getBooleanExtra(EXTRA_FILTER_DONT_SHOW_NO_FILTER, false)) {
				bNoFilter.setVisibility(View.GONE);
			}
		}		
	}
	
	private void selectPeriod(Period p) {
		spinnerPeriodType.setSelection(p.type.ordinal());
	}

	public static int selectPeriodType(String s) {
		PeriodType[] periods = PeriodType.values(); 
		for (PeriodType p : periods) {
			if (p.name().equals(s)) {
				return p.ordinal();
			}
		}
		return -1;
	}		

	private void selectPeriod(long from, long to) {
		cFrom.setTimeInMillis(from);
		cTo.setTimeInMillis(to);			
		spinnerPeriodType.setSelection(PeriodType.CUSTOM.ordinal());
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		final Dialog d = new Dialog(this);
		d.setCancelable(true);
		d.setTitle(id == 1 ? R.string.period_from : R.string.period_to);
		d.setContentView(R.layout.filter_period_select);
		Button bOk = (Button)d.findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setDialogResult(d, id == 1 ? cFrom : cTo);
				d.dismiss();
			}
		});		
		Button bCancel = (Button)d.findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				d.cancel();
			}
		});
		return d;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		prepareDialog(dialog, id == 1 ? cFrom : cTo);
	}

	private void prepareDialog(Dialog dialog, Calendar c) {
		DatePicker dp = (DatePicker)dialog.findViewById(R.id.date);
		dp.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), null);
		TimePicker tp = (TimePicker)dialog.findViewById(R.id.time);
		tp.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
		tp.setCurrentMinute(c.get(Calendar.MINUTE));
        tp.setIs24HourView(is24HourFormat(this));
		tp.setIs24HourView(true);
	}

	private void setDialogResult(Dialog d, Calendar c) {
		DatePicker dp = (DatePicker)d.findViewById(R.id.date);
		c.set(Calendar.YEAR, dp.getYear());
		c.set(Calendar.MONTH, dp.getMonth());
		c.set(Calendar.DAY_OF_MONTH, dp.getDayOfMonth());
		TimePicker tp = (TimePicker)d.findViewById(R.id.time);
		c.set(Calendar.HOUR_OF_DAY, tp.getCurrentHour());
		c.set(Calendar.MINUTE, tp.getCurrentMinute());
		updateDate();
	}

	private void enableButtons() {
		buttonPeriodFrom.setEnabled(true);
		buttonPeriodTo.setEnabled(true);
	}

	private void disableButtons() {
		buttonPeriodFrom.setEnabled(false);
		buttonPeriodTo.setEnabled(false);
	}

	private void updateDate(Period p) {
		cFrom.setTimeInMillis(p.start);
		cTo.setTimeInMillis(p.end);
		updateDate();
	}
	
	private void updateDate() {
		buttonPeriodFrom.setText(df.format(cFrom.getTime()));
		buttonPeriodTo.setText(df.format(cTo.getTime()));
	}

	protected void selectToday() {
		disableButtons();
		updateDate(DateUtils.today());
	}

	protected void selectYesterday() {
		disableButtons();
		updateDate(DateUtils.yesterday());
	}

	protected void selectThisWeek() {
		disableButtons();
		updateDate(DateUtils.thisWeek());
	}

	protected void selectThisMonth() {
		disableButtons();
		updateDate(DateUtils.thisMonth());
	}

	protected void selectLastWeek() {
		disableButtons();
		updateDate(DateUtils.lastWeek());
	}

	protected void selectLastMonth() {
		disableButtons();
		updateDate(DateUtils.lastMonth());
	}

	protected void selectCustom() {
		updateDate();
		enableButtons();				
	}

	private void reset() {
		spinnerPeriodType.setSelection(0);		
	}

}
