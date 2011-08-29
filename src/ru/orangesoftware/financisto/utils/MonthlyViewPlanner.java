/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import android.database.Cursor;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.recur.Recurrence;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/25/11 11:00 PM
 */
public class MonthlyViewPlanner {

    public static final Transaction PAYMENTS_HEADER = new Transaction();
    public static final Transaction CREDITS_HEADER = new Transaction();
    public static final Transaction EXPENSES_HEADER = new Transaction();

    static {
        PAYMENTS_HEADER.dateTime = 0;
        CREDITS_HEADER.dateTime = 0;
        EXPENSES_HEADER.dateTime = 0;
    }

    private final DatabaseAdapter db;
    private final long accountId;
    private final Date startDate;
    private final Date endDate;
    private final Date now;

    public MonthlyViewPlanner(DatabaseAdapter db, long accountId, Date startDate, Date endDate, Date now) {
        this.db = db;
        this.accountId = accountId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.now = now;
    }

    /**
     * [Monthly view] Returns all the transactions for a given Account in a given period (month).
     * @return Transactions of the given Account, from start date to end date.
     */
    public List<Transaction> getAccountMonthlyView() {
        WhereFilter filter = createMonthlyViewFilter();
        List<Transaction> regularTransactions = asTransactionList(db.getBlotterForAccountWithSplits(filter));
        List<Transaction> scheduledTransactions = asTransactionList(db.getAllScheduledTransactions());
        if (scheduledTransactions.isEmpty()) {
            return regularTransactions;
        } else {
            List<Transaction> allTransactions = new ArrayList<Transaction>();
            allTransactions.addAll(regularTransactions);
            allTransactions.addAll(planSchedules(scheduledTransactions));
            sortTransactions(allTransactions);
            return allTransactions;
        }
    }

    private List<Transaction> planSchedules(List<Transaction> scheduledTransactions) {
        List<Transaction> plannedTransactions = new ArrayList<Transaction>();
        for (Transaction scheduledTransaction : scheduledTransactions) {
            Transaction transaction = inverseTransaction(scheduledTransaction);
            if (transaction.fromAccountId == accountId) {
                List<Date> dates = calculatePlannedDates(transaction);
                duplicateTransaction(transaction, dates, plannedTransactions);
            } else if (transaction.isSplitParent()) {
                planSplitSchedules(transaction, plannedTransactions);
            }
        }
        return plannedTransactions;
    }

    private Transaction inverseTransaction(Transaction transaction) {
        if (transaction.toAccountId == accountId) {
            Transaction inverse = transaction.clone();
            inverse.fromAccountId = transaction.toAccountId;
            inverse.fromAmount = transaction.toAmount;
            inverse.toAccountId = transaction.fromAccountId;
            inverse.toAmount = transaction.fromAmount;
            return inverse;
        }
        return transaction;
    }

    private void planSplitSchedules(Transaction scheduledTransaction, List<Transaction> plannedTransactions) {
        List<Date> dates = calculatePlannedDates(scheduledTransaction);
        List<Transaction> splits = db.em().getSplitsForTransaction(scheduledTransaction.id);
        for (Transaction split : splits) {
            if (split.toAccountId == accountId) {
                Transaction transaction = inverseTransaction(split);
                duplicateTransaction(transaction, dates, plannedTransactions);
            }
        }
    }

    private List<Date> calculatePlannedDates(Transaction scheduledTransaction) {
        String recurrence = scheduledTransaction.recurrence;
        Date calcDate = startDate.before(now) ? now : startDate;
        if (recurrence == null) {
            Date scheduledDate = new Date(scheduledTransaction.dateTime);
            if (scheduledDate.after(calcDate) || scheduledDate.equals(calcDate)) {
                return Collections.singletonList(scheduledDate);
            }
        } else {
            Recurrence r = Recurrence.parse(recurrence);
            return r.generateDates(calcDate, endDate);
        }
        return Collections.emptyList();
    }

    private void duplicateTransaction(Transaction scheduledTransaction, List<Date> dates, List<Transaction> plannedTransactions) {
        if (dates.size() == 1) {
            scheduledTransaction.dateTime = dates.get(0).getTime();
            scheduledTransaction.isTemplate = 0;
            plannedTransactions.add(scheduledTransaction);
        } else {
            for (Date date : dates) {
                Transaction t = scheduledTransaction.clone();
                t.dateTime = date.getTime();
                t.isTemplate = 0;
                plannedTransactions.add(t);
            }
        }
    }

    private void sortTransactions(List<Transaction> transactions) {
        Collections.sort(transactions, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction transaction1, Transaction transaction2) {
                return transaction1.dateTime > transaction2.dateTime ? 1 : (transaction1.dateTime < transaction2.dateTime ? -1 : 0);
            }
        });
    }

    private List<Transaction> asTransactionList(Cursor cursor) {
        try {
            List<Transaction> transactions = new ArrayList<Transaction>(cursor.getCount());
            while (cursor.moveToNext()) {
                transactions.add(Transaction.fromBlotterCursor(cursor));
            }
            return transactions;
        } finally {
            cursor.close();
        }
    }

    private WhereFilter createMonthlyViewFilter() {
        return WhereFilter.empty()
                .eq(DatabaseHelper.BlotterColumns.from_account_id.name(), String.valueOf(accountId))
                .btw(DatabaseHelper.BlotterColumns.datetime.name(), String.valueOf(startDate.getTime()), String.valueOf(endDate.getTime()))
                .eq(WhereFilter.Criteria.raw("("+ DatabaseHelper.TransactionColumns.parent_id+"=0 OR "+ DatabaseHelper.BlotterColumns.is_transfer+"=-1)"))
                .asc(DatabaseHelper.BlotterColumns.datetime.name());
    }


    public List<Transaction> getCreditCardStatement() {
        List<Transaction> transactions = getAccountMonthlyView();
        List<Transaction> statement = new ArrayList<Transaction>(transactions.size()+3);
        // add payments
        statement.add(PAYMENTS_HEADER);
        for (Transaction transaction : transactions) {
            if (transaction.isCreditCardPayment() && transaction.fromAmount > 0) {
                statement.add(transaction);
            }
        }
        // add credits
        statement.add(CREDITS_HEADER);
        for (Transaction transaction : transactions) {
            if (!transaction.isCreditCardPayment() && transaction.fromAmount > 0) {
                statement.add(transaction);
            }
        }
        // add expenses
        statement.add(EXPENSES_HEADER);
        for (Transaction transaction : transactions) {
            if (transaction.fromAmount < 0) {
                statement.add(transaction);
            }
        }
        return statement;
    }
}
