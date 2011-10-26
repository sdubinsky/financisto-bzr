/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.imports.csv;

import android.content.Intent;
import ru.orangesoftware.financisto.activity.CsvImportActivity;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyExportPreferences;

import java.text.SimpleDateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/10/11 7:01 PM
 */
public class CsvImportOptions {

    public static final String DEFAULT_DATE_FORMAT = "dd.MM.yyyy";

    public final Currency currency;
    public final SimpleDateFormat dateFormat;
    public final char fieldSeparator;
    public final WhereFilter filter;
    public final long[] selectedAccounts;
    public final String filename;

    public CsvImportOptions(Currency currency, String dateFormat, long[] selectedAccounts, WhereFilter filter, String filename, char fieldSeparator) {
        this.currency = currency;
        this.dateFormat = new SimpleDateFormat(dateFormat);
        this.selectedAccounts = selectedAccounts;
        this.filter = filter;
        this.filename = filename;
        this.fieldSeparator = fieldSeparator;
    }

    public static CsvImportOptions fromIntent(Intent data) {
        WhereFilter filter = WhereFilter.fromIntent(data);
        Currency currency = CurrencyExportPreferences.fromIntent(data, "csv");
        char fieldSeparator = data.getCharExtra(CsvImportActivity.CSV_IMPORT_FIELD_SEPARATOR, ',');
        String dateFormat = data.getStringExtra(CsvImportActivity.CSV_IMPORT_DATE_FORMAT);
        long[] selectedAccounts = data.getLongArrayExtra(CsvImportActivity.CSV_IMPORT_SELECTED_ACCOUNT);
        String filename = data.getStringExtra(CsvImportActivity.CSV_IMPORT_FILENAME);
        return new CsvImportOptions(currency, dateFormat, selectedAccounts, filter, filename, fieldSeparator);
    }

}
