/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import android.util.Log;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/1/11 11:54 PM
 */
public class TransactionsTotalCalculator {

    public static final String[] BALANCE_PROJECTION = {
        "from_account_currency_id",
        "SUM(from_amount)"};

    public static final String BALANCE_GROUPBY = "from_account_currency_id";

    public static final String[] HOME_CURRENCY_PROJECTION = {
            "datetime",
            "from_account_currency_id",
            "from_amount",
            "to_account_currency_id",
            "to_amount"
    };

    private final DatabaseAdapter db;
    private final WhereFilter filter;

    public TransactionsTotalCalculator(DatabaseAdapter db, WhereFilter filter) {
        this.db = db;
        this.filter = filter;
    }

    public Total[] getTransactionsBalance() {
        WhereFilter filter = this.filter;
        if (filter.getAccountId() == -1) {
            filter = excludeAccountsNotIncludedInTotalsAndSplits(filter);
        }
        Cursor c = db.db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, BALANCE_PROJECTION,
                filter.getSelection(), filter.getSelectionArgs(),
                BALANCE_GROUPBY, null, null);
        try {
            int count = c.getCount();
            List<Total> totals = new ArrayList<Total>(count);
            while (c.moveToNext()) {
                long currencyId = c.getLong(0);
                long balance = c.getLong(1);
                Currency currency = CurrencyCache.getCurrency(db.em(), currencyId);
                Total total = new Total(currency);
                total.balance = balance;
                totals.add(total);
            }
            return totals.toArray(new Total[totals.size()]);
        } finally {
            c.close();
        }
    }

    public Total getAccountTotal() {
        Total[] totals = getTransactionsBalance();
        return totals.length > 0 ? totals[0] : Total.ZERO;
    }

    public Total getBlotterBalanceInHomeCurrency() {
        Currency homeCurrency = db.em().getHomeCurrency();
        Total total = new Total(homeCurrency);
        total.balance = getBlotterBalance(homeCurrency);
        return total;
    }

    public long getBlotterBalance(Currency toCurrency) {
        WhereFilter filter = excludeAccountsNotIncludedInTotalsAndSplits(this.filter);
        return getBalanceInHomeCurrency(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, toCurrency, filter);
    }

    public long getAccountBalance(Currency toCurrency, long accountId) {
        WhereFilter filter = selectedAccountOnly(this.filter, accountId);
        return getBalanceInHomeCurrency(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, toCurrency, filter);
    }

    private WhereFilter selectedAccountOnly(WhereFilter filter, long accountId) {
        WhereFilter copy = WhereFilter.copyOf(filter);
        copy.put(WhereFilter.Criteria.eq("from_account_id", String.valueOf(accountId)));
        return copy;
    }

    private long getBalanceInHomeCurrency(String view, Currency toCurrency, WhereFilter filter) {
        Log.d("Financisto", "Query balance: "+filter.getSelection()+" => "+ Arrays.toString(filter.getSelectionArgs()));
        Cursor c = db.db().query(view, HOME_CURRENCY_PROJECTION,
                filter.getSelection(), filter.getSelectionArgs(),
                null, null, null);
        try {
            return calculateTotalFromCursor(db, c, toCurrency);
        } finally {
            c.close();
        }
    }

    public static long calculateTotalFromCursor(DatabaseAdapter db, Cursor c, Currency toCurrency) {
        MyEntityManager em = db.em();
        ExchangeRateProvider rates = db.getHistoryRates();
        float balance = 0;
        while (c.moveToNext()) {
            balance += getAmountFromCursor(em, c, toCurrency, rates, 0);
        }
        return (long)balance;
    }

    public static float getAmountFromCursor(MyEntityManager em, Cursor c, Currency toCurrency, ExchangeRateProvider rates, int index) {
        long datetime = c.getLong(index++);
        long fromCurrencyId = c.getLong(index++);
        long fromAmount = c.getLong(index++);
        long toCurrencyId = c.getLong(index++);
        long toAmount = c.getLong(index);
        if (fromCurrencyId == toCurrency.id) {
            return fromAmount;
        } else if (toCurrencyId > 0 && toCurrencyId == toCurrency.id) {
            return -toAmount;
        } else {
            Currency fromCurrency = CurrencyCache.getCurrency(em, fromCurrencyId);
            float rate = rates.getRate(fromCurrency, toCurrency, datetime).rate;
            return rate*fromAmount;
        }
    }

    private WhereFilter excludeAccountsNotIncludedInTotalsAndSplits(WhereFilter filter) {
        WhereFilter copy = WhereFilter.copyOf(filter);
        copy.eq("from_account_is_include_into_totals", "1");
        copy.neq("category_id", "-1");
        return copy;
    }

}
