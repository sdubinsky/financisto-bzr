/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.model.rates;

import android.content.ContentValues;
import android.database.Cursor;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.DateUtils;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/17/12 11:25 PM
 */
public class ExchangeRate implements Comparable<ExchangeRate> {

    public static ExchangeRate createDefaultRate(Currency fromCurrency, Currency toCurrency) {
        ExchangeRate rate;
        rate = new ExchangeRate();
        rate.fromCurrencyId = fromCurrency.id;
        rate.toCurrencyId = toCurrency.id;
        rate.date = DateUtils.atMidnight(System.currentTimeMillis());
        rate.rate = 1;
        return rate;
    }

    public static ExchangeRate fromCursor(Cursor c) {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = c.getLong(DatabaseHelper.ExchangeRateColumns.from_currency_id.ordinal());
        r.toCurrencyId = c.getLong(DatabaseHelper.ExchangeRateColumns.to_currency_id.ordinal());
        r.date = c.getLong(DatabaseHelper.ExchangeRateColumns.rate_date.ordinal());
        r.rate = c.getFloat(DatabaseHelper.ExchangeRateColumns.rate.ordinal());
        return r;
    }

    public ContentValues toValues() {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ExchangeRateColumns.from_currency_id.name(), fromCurrencyId);
        values.put(DatabaseHelper.ExchangeRateColumns.to_currency_id.name(), toCurrencyId);
        values.put(DatabaseHelper.ExchangeRateColumns.rate_date.name(), date);
        values.put(DatabaseHelper.ExchangeRateColumns.rate.name(), rate);
        return values;
    }

    public long fromCurrencyId;
    public long toCurrencyId;
    public long date;
    public float rate;

    public ExchangeRate flip() {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = toCurrencyId;
        r.toCurrencyId = fromCurrencyId;
        r.date = date;
        r.rate = 1.0f/rate;
        return r;
    }

    @Override
    public int compareTo(ExchangeRate that) {
        long d0 = this.date;
        long d1 = that.date;
        return d0 > d1 ? -1 : (d0 < d1 ? 1 : 0);
    }

}
