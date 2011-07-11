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

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.Spinner;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.CurrencyExportPreferences;

public class CsvExportActivity extends AbstractExportActivity {
	
	public static final String CSV_EXPORT_DATE_FORMAT = "CSV_EXPORT_DATE_FORMAT";
	public static final String CSV_EXPORT_TIME_FORMAT = "CSV_EXPORT_TIME_FORMAT";
	public static final String CSV_EXPORT_FIELD_SEPARATOR = "CSV_EXPORT_FIELD_SEPARATOR";
	public static final String CSV_EXPORT_INCLUDE_HEADER = "CSV_EXPORT_INCLUDE_HEADER";

    private final CurrencyExportPreferences currencyPreferences = new CurrencyExportPreferences("csv");

    public CsvExportActivity() {
        super(R.layout.csv_export);
    }

    @Override
    protected void internalOnCreate() {
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        currencyPreferences.updateIntentFromUI(this, data);
        Spinner fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
        CheckBox includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
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
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        currencyPreferences.savePreferences(this, editor);

        Spinner fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
        CheckBox includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
		editor.putInt(CSV_EXPORT_FIELD_SEPARATOR, fieldSeparators.getSelectedItemPosition());
		editor.putBoolean(CSV_EXPORT_INCLUDE_HEADER, includeHeader.isChecked());
		
		editor.commit();
	}
	
	private void restorePreferences() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        currencyPreferences.restorePreferences(this, prefs);

        Spinner fieldSeparators = (Spinner)findViewById(R.id.spinnerFieldSeparator);
        CheckBox includeHeader = (CheckBox)findViewById(R.id.checkboxIncludeHeader);
        fieldSeparators.setSelection(prefs.getInt(CSV_EXPORT_FIELD_SEPARATOR, 0));
		includeHeader.setChecked(prefs.getBoolean(CSV_EXPORT_INCLUDE_HEADER, true));
	}

}
