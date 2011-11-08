/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.qif;

import android.content.Intent;
import ru.orangesoftware.financisto.activity.QifImportActivity;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import java.text.SimpleDateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/10/11 7:01 PM
 */
public class QifImportOptions {

    public final SimpleDateFormat dateFormat;
    public final String filename;
    public final Currency currency;

    public QifImportOptions(String filename, String dateFormat, Currency currency) {
        this.filename = filename;
        this.dateFormat = new SimpleDateFormat(dateFormat);
        this.currency = currency;
    }

    public static QifImportOptions fromIntent(Intent data) {
        String filename = data.getStringExtra(QifImportActivity.QIF_IMPORT_FILENAME);
        String dateFormat = data.getStringExtra(QifImportActivity.QIF_IMPORT_DATE_FORMAT);
        long currencyId = data.getLongExtra(QifImportActivity.QIF_IMPORT_CURRENCY, 1);
        Currency currency = CurrencyCache.getCurrencyOrEmpty(currencyId);
        return new QifImportOptions(filename, dateFormat, currency);
    }

}
