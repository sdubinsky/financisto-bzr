/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import android.database.Cursor;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.model.TransactionInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/25/11 11:00 PM
 */
public class MonthlyViewPlanner extends AbstractPlanner {

    public static final TransactionInfo PAYMENTS_HEADER = new TransactionInfo();
    public static final TransactionInfo CREDITS_HEADER = new TransactionInfo();
    public static final TransactionInfo EXPENSES_HEADER = new TransactionInfo();

    static {
        PAYMENTS_HEADER.dateTime = 0;
        CREDITS_HEADER.dateTime = 0;
        EXPENSES_HEADER.dateTime = 0;
    }

    private final long accountId;

    public MonthlyViewPlanner(DatabaseAdapter db, long accountId, Date startDate, Date endDate, Date now) {
        super(db, createMonthlyViewFilter(startDate, endDate, accountId), now);
        this.accountId = accountId;
    }

    @Override
    protected Cursor getRegularTransactions() {
        WhereFilter blotterFilter = WhereFilter.copyOf(filter);
        return db.getBlotterForAccountWithSplits(blotterFilter);
    }

    private static WhereFilter createMonthlyViewFilter(Date startDate, Date endDate, long accountId) {
        WhereFilter filter = WhereFilter.empty();
        filter.put(new DateTimeCriteria(startDate.getTime(), endDate.getTime()));
        filter.eq(DatabaseHelper.BlotterColumns.from_account_id.name(), String.valueOf(accountId));
        filter.eq(Criteria.raw("(" + DatabaseHelper.TransactionColumns.parent_id + "=0 OR " + DatabaseHelper.BlotterColumns.is_transfer + "=-1)"));
        filter.asc(DatabaseHelper.BlotterColumns.datetime.name());
        return filter;
    }

    @Override
    protected TransactionInfo prepareScheduledTransaction(TransactionInfo scheduledTransaction) {
        return inverseTransaction(scheduledTransaction);
    }

    @Override
    protected boolean includeScheduledTransaction(TransactionInfo transaction) {
        return transaction.fromAccount.id == accountId;
    }

    @Override
    protected boolean includeScheduledSplitTransaction(TransactionInfo split) {
        return split.isTransfer() && split.toAccount.id == accountId;
    }

    private TransactionInfo inverseTransaction(TransactionInfo transaction) {
        if (transaction.isTransfer() && transaction.toAccount.id == accountId) {
            TransactionInfo inverse = transaction.clone();
            inverse.fromAccount = transaction.toAccount;
            inverse.fromAmount = transaction.toAmount;
            inverse.toAccount = transaction.fromAccount;
            inverse.toAmount = transaction.fromAmount;
            return inverse;
        }
        return transaction;
    }


    public List<TransactionInfo> getCreditCardStatement() {
        List<TransactionInfo> transactions = getPlannedTransactions();
        List<TransactionInfo> statement = new ArrayList<TransactionInfo>(transactions.size()+3);
        // add payments
        statement.add(PAYMENTS_HEADER);
        for (TransactionInfo transaction : transactions) {
            if (transaction.isCreditCardPayment() && transaction.fromAmount > 0) {
                statement.add(transaction);
            }
        }
        // add credits
        statement.add(CREDITS_HEADER);
        for (TransactionInfo transaction : transactions) {
            if (!transaction.isCreditCardPayment() && transaction.fromAmount > 0) {
                statement.add(transaction);
            }
        }
        // add expenses
        statement.add(EXPENSES_HEADER);
        for (TransactionInfo transaction : transactions) {
            if (transaction.fromAmount < 0) {
                statement.add(transaction);
            }
        }
        return statement;
    }

}
