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

import android.database.AbstractCursor;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.DateUtils.Period;
import ru.orangesoftware.financisto.utils.DateUtils.PeriodType;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

public class CsvExportActivity extends AbstractExportActivity {
	
	public static final String CSV_EXPORT_DECIMALS = "CSV_EXPORT_DECIMALS";
	public static final String CSV_EXPORT_DECIMAL_SEPARATOR = "CSV_EXPORT_DECIMAL_SEPARATOR";
	public static final String CSV_EXPORT_GROUP_SEPARATOR = "CSV_EXPORT_GROUP_SEPARATOR";
	public static final String CSV_EXPORT_DATE_FORMAT = "CSV_EXPORT_DATE_FORMAT";
	public static final String CSV_EXPORT_TIME_FORMAT = "CSV_EXPORT_TIME_FORMAT";
	public static final String CSV_EXPORT_FIELD_SEPARATOR = "CSV_EXPORT_FIELD_SEPARATOR";
	public static final String CSV_EXPORT_INCLUDE_HEADER = "CSV_EXPORT_INCLUDE_HEADER";

    public CsvExportActivity() {
        super(R.layout.csv_export);
    }

    @Override
    protected void internalOnCreate() {
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        Spinner decimals = (Spinner)findViewById(R.id.spinnerDecimals);
        Spinner decimalSeparators = (Spinner)findViewById(R.id.spinnerDecimalSeparators);
        Spinner groupSeparators = (Spinner)findViewById(R.id.spinnerGroupSeparators);
        Spinner fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
        CheckBox includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
        data.putExtra(CSV_EXPORT_DECIMALS, 2-decimals.getSelectedItemPosition());
        data.putExtra(CSV_EXPORT_DECIMAL_SEPARATOR, decimalSeparators.getSelectedItem().toString());
        data.putExtra(CSV_EXPORT_GROUP_SEPARATOR, groupSeparators.getSelectedItem().toString());
        data.putExtra(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItem().toString().charAt(1));
        data.putExtra(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
    }

    @Override
	protected void onPause() {
		super.onPause();
		savePreferences();
	}

	@Override
	protected void onResume() {
		super.onResume();
		restorePreferences();
	}
	
	private void savePreferences() {
		Spinner decimals = (Spinner)findViewById(R.id.spinnerDecimals);
		Spinner decimalSeparators = (Spinner)findViewById(R.id.spinnerDecimalSeparators);
		Spinner groupSeparators = (Spinner)findViewById(R.id.spinnerGroupSeparators);
		Spinner fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
		CheckBox includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);

		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		
		editor.putInt(CSV_EXPORT_DECIMALS, decimals.getSelectedItemPosition());
		editor.putInt(CSV_EXPORT_DECIMAL_SEPARATOR, decimalSeparators.getSelectedItemPosition());
		editor.putInt(CSV_EXPORT_GROUP_SEPARATOR, groupSeparators.getSelectedItemPosition());
		editor.putInt(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItemPosition());
		editor.putBoolean(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
		
		editor.commit();
	}
	
	private void restorePreferences() {
		Spinner decimals = (Spinner)findViewById(R.id.spinnerDecimals);
		Spinner decimalSeparators = (Spinner)findViewById(R.id.spinnerDecimalSeparators);
		Spinner groupSeparators = (Spinner)findViewById(R.id.spinnerGroupSeparators);
		Spinner fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
		CheckBox includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		
		decimals.setSelection(prefs.getInt(CSV_EXPORT_DECIMALS, 0));
		decimalSeparators.setSelection(prefs.getInt(CSV_EXPORT_DECIMAL_SEPARATOR, 0));
		groupSeparators.setSelection(prefs.getInt(CSV_EXPORT_GROUP_SEPARATOR, 1));
		fieldSeparators.setSelection(prefs.getInt(CSV_EXPORT_FIELD_SEPARATOR, 0));
		includeHeader.setChecked(prefs.getBoolean(CSV_EXPORT_INCLUDE_HEADER, true));
	}

}
