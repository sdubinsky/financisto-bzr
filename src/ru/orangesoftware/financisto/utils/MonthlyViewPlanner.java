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
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.recur.Recurrence;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/25/11 11:00 PM
 */
public class MonthlyViewPlanner {

    public static final TransactionInfo PAYMENTS_HEADER = new TransactionInfo();
    public static final TransactionInfo CREDITS_HEADER = new TransactionInfo();
    public static final TransactionInfo EXPENSES_HEADER = new TransactionInfo();

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
    private final MyEntityManager em;

    public MonthlyViewPlanner(DatabaseAdapter db, long accountId, Date startDate, Date endDate, Date now) {
        this.db = db;
        this.em = db.em();
        this.accountId = accountId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.now = now;
    }

    /**
     * [Monthly view] Returns all the transactions for a given Account in a given period (month).
     * @return Transactions of the given Account, from start date to end date.
     */
    public List<TransactionInfo> getAccountMonthlyView() {
        WhereFilter filter = createMonthlyViewFilter();
        List<TransactionInfo> regularTransactions = asTransactionList(db.getBlotterForAccountWithSplits(filter));
        List<TransactionInfo> scheduledTransactions = getScheduledTransactions();
        if (scheduledTransactions.isEmpty()) {
            return regularTransactions;
        } else {
            List<TransactionInfo> allTransactions = new ArrayList<TransactionInfo>();
            allTransactions.addAll(regularTransactions);
            allTransactions.addAll(planSchedules(scheduledTransactions));
            sortTransactions(allTransactions);
            return allTransactions;
        }
    }

    private List<TransactionInfo> getScheduledTransactions() {
        if (now.before(endDate)) {
            return em.getAllScheduledTransactions();
        }
        return Collections.emptyList();
    }

    private List<TransactionInfo> planSchedules(List<TransactionInfo> scheduledTransactions) {
        List<TransactionInfo> plannedTransactions = new ArrayList<TransactionInfo>();
        for (TransactionInfo scheduledTransaction : scheduledTransactions) {
            TransactionInfo transaction = inverseTransaction(scheduledTransaction);
            if (transaction.fromAccount.id == accountId) {
                List<Date> dates = calculatePlannedDates(transaction);
                duplicateTransaction(transaction, dates, plannedTransactions);
            } else if (transaction.isSplitParent()) {
                planSplitSchedules(transaction, plannedTransactions);
            }
        }
        return plannedTransactions;
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

    private void planSplitSchedules(TransactionInfo scheduledTransaction, List<TransactionInfo> plannedTransactions) {
        List<Date> dates = calculatePlannedDates(scheduledTransaction);
        List<TransactionInfo> splits = em.getSplitsInfoForTransaction(scheduledTransaction.id);
        for (TransactionInfo split : splits) {
            if (split.isTransfer() && split.toAccount.id == accountId) {
                TransactionInfo transaction = inverseTransaction(split);
                duplicateTransaction(transaction, dates, plannedTransactions);
            }
        }
    }

    private List<Date> calculatePlannedDates(TransactionInfo scheduledTransaction) {
        String recurrence = scheduledTransaction.recurrence;
        Date calcDate = startDate.before(now) ? now : startDate;
        if (recurrence == null) {
            Date scheduledDate = new Date(scheduledTransaction.dateTime);
            if (insideTheRequiredPeriod(calcDate, endDate, scheduledDate)) {
                return Collections.singletonList(scheduledDate);
            }
        } else {
            Recurrence r = Recurrence.parse(recurrence);
            return r.generateDates(calcDate, endDate);
        }
        return Collections.emptyList();
    }

    private boolean insideTheRequiredPeriod(Date startDate, Date endDate, Date date) {
        return !(date.before(startDate) || date.after(endDate));
    }

    private void duplicateTransaction(TransactionInfo scheduledTransaction, List<Date> dates, List<TransactionInfo> plannedTransactions) {
        if (dates.size() == 1) {
            scheduledTransaction.dateTime = dates.get(0).getTime();
            scheduledTransaction.isTemplate = 0;
            plannedTransactions.add(scheduledTransaction);
        } else {
            for (Date date : dates) {
                TransactionInfo t = scheduledTransaction.clone();
                t.dateTime = date.getTime();
                t.isTemplate = 0;
                plannedTransactions.add(t);
            }
        }
    }

    private void sortTransactions(List<TransactionInfo> transactions) {
        Collections.sort(transactions, new Comparator<TransactionInfo>() {
            @Override
            public int compare(TransactionInfo transaction1, TransactionInfo transaction2) {
                return transaction1.dateTime > transaction2.dateTime ? 1 : (transaction1.dateTime < transaction2.dateTime ? -1 : 0);
            }
        });
    }

    private List<TransactionInfo> asTransactionList(Cursor cursor) {
        try {
            List<TransactionInfo> transactions = new ArrayList<TransactionInfo>(cursor.getCount());
            while (cursor.moveToNext()) {
                transactions.add(TransactionInfo.fromBlotterCursor(cursor));
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


    public List<TransactionInfo> getCreditCardStatement() {
        List<TransactionInfo> transactions = getAccountMonthlyView();
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
