/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import java.util.ArrayList;
import java.util.List;

import static ru.orangesoftware.financisto.db.DatabaseAdapter.enhanceFilterForAccountBlotter;

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

    private final DatabaseAdapter db;
    private final WhereFilter filter;

    public TransactionsTotalCalculator(DatabaseAdapter db, WhereFilter filter) {
        this.db = db;
        this.filter = filter;
    }

    public Total[] getTransactionsBalance() {
        WhereFilter blotterFilter = enhanceFilterForAccountBlotter(filter);
        Cursor c = db.db().query(DatabaseHelper.V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, BALANCE_PROJECTION,
                blotterFilter.getSelection(), blotterFilter.getSelectionArgs(),
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

}
