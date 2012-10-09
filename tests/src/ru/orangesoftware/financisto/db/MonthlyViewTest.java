/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.util.Log;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.test.*;
import ru.orangesoftware.financisto.utils.MonthlyViewPlanner;
import ru.orangesoftware.financisto.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MonthlyViewTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    Date from = DateTime.date(2011, 8, 1).atMidnight().asDate();
    Date to = DateTime.date(2011, 8, 16).atDayEnd().asDate();
    Date now = DateTime.date(2011, 8, 8).at(23, 20, 0, 0).asDate();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        a2 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
    }

    public void test_should_generate_monthly_view_for_account() {
        prepareData();
        //a1
        //2011-08-08 +1000          t1
        //2011-08-09 -100 -> a2     t2
        //2011-08-09 +40            r2
        //2011-08-10 -500           t3
        //2011-08-10 -50            r1
        //2011-08-11 -100 -> a2     t4
        //2011-08-12 +200 <- a2     t5
        //2011-08-12 +52  <- a2     r4
        //2011-08-12 -50            r1
        //2011-08-12 +30  <- a2     r6
        //2011-08-14 -100           t7
        //2011-08-14 -50            r1
        //2011-08-15 +400           t6
        //2011-08-15 -210 -> a2     r3
        //2011-08-15 -105 -> a2     r5
        //2011-08-16 -50            r1
        //2011-08-16 +40            r2
        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1.id, from, to, now);
        List<TransactionInfo> transactions = planner.getAccountMonthlyView();
        logTransactions(transactions);
        assertTransactions(transactions,
                DateTime.date(2011, 8, 8), 1000,
                DateTime.date(2011, 8, 9), -100,
                DateTime.date(2011, 8, 9), 40,
                DateTime.date(2011, 8, 10), -500,
                DateTime.date(2011, 8, 10), -50,
                DateTime.date(2011, 8, 11), -100,
                DateTime.date(2011, 8, 12), 200,
                DateTime.date(2011, 8, 12), 52,
                DateTime.date(2011, 8, 12), -50,
                DateTime.date(2011, 8, 12), 30,
                DateTime.date(2011, 8, 14), -100,
                DateTime.date(2011, 8, 14), -50,
                DateTime.date(2011, 8, 15), 400,
                DateTime.date(2011, 8, 15), -210,
                DateTime.date(2011, 8, 15), -105,
                DateTime.date(2011, 8, 16), -50,
                DateTime.date(2011, 8, 16), 40
        );
    }

    public void test_should_generate_credit_card_statement() {
        prepareData();
        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1.id, from, to, now);
        List<TransactionInfo> transactions = planner.getCreditCardStatement();
        logTransactions(transactions);
        assertTransactions(transactions,
                //payments
                DateTime.NULL_DATE, 0,
                DateTime.date(2011, 8, 15), 400,
                //credits
                DateTime.NULL_DATE, 0,
                DateTime.date(2011, 8, 8), 1000,
                DateTime.date(2011, 8, 9), 40,
                DateTime.date(2011, 8, 12), 200,
                DateTime.date(2011, 8, 12), 52,
                DateTime.date(2011, 8, 12), 30,
                DateTime.date(2011, 8, 16), 40,
                //expenses
                DateTime.NULL_DATE, 0,
                DateTime.date(2011, 8, 9), -100,
                DateTime.date(2011, 8, 10), -500,
                DateTime.date(2011, 8, 10), -50,
                DateTime.date(2011, 8, 11), -100,
                DateTime.date(2011, 8, 12), -50,
                DateTime.date(2011, 8, 14), -100,
                DateTime.date(2011, 8, 14), -50,
                DateTime.date(2011, 8, 15), -210,
                DateTime.date(2011, 8, 15), -105,
                DateTime.date(2011, 8, 16), -50
        );
    }

    public void test_should_generate_monthly_preview_for_the_next_month_correctly(){
        prepareData();
        from = DateTime.date(2011, 9, 1).atMidnight().asDate();
        to = DateTime.date(2011, 9, 16).atDayEnd().asDate();

        //2011-09-02 -50            r1
        //2011-09-02 +52  <- a2     r4
        //2011-09-02 +30  <- a2     r6
        //2011-09-04 -50            r1
        //2011-09-06 -50            r1
        //2011-08-06 +40            r2
        //2011-09-08 -50            r1
        //2011-09-09 +52  <- a2     r4
        //2011-09-09 +30  <- a2     r6
        //2011-09-10 -50            r1
        //2011-09-12 -50            r1
        //2011-09-13 +40            r2
        //2011-09-14 -50            r1
        //2011-09-16 -50            r1
        //2011-09-16 +52  <- a2     r4
        //2011-09-16 +30  <- a2     r6
        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1.id, from, to, now);
        List<TransactionInfo> transactions = planner.getAccountMonthlyView();
        logTransactions(transactions);
        assertTransactions(transactions,
                DateTime.date(2011, 9, 1), -50,
                DateTime.date(2011, 9, 2), 52,
                DateTime.date(2011, 9, 2), 30,
                DateTime.date(2011, 9, 3), -50,
                DateTime.date(2011, 9, 5), -50,
                DateTime.date(2011, 9, 6), 40,
                DateTime.date(2011, 9, 7), -50,
                DateTime.date(2011, 9, 9), 52,
                DateTime.date(2011, 9, 9), -50,
                DateTime.date(2011, 9, 9), 30,
                DateTime.date(2011, 9, 11), -50,
                DateTime.date(2011, 9, 13), -50,
                DateTime.date(2011, 9, 13), 40,
                DateTime.date(2011, 9, 15), -50,
                DateTime.date(2011, 9, 16), 52,
                DateTime.date(2011, 9, 16), 30
        );

    }

    public void test_should_generate_monthly_preview_for_the_previous_month_correctly(){
        prepareData();
        from = DateTime.date(2011, 7, 1).atMidnight().asDate();
        to = DateTime.date(2011, 7, 16).atDayEnd().asDate();

        MonthlyViewPlanner planner = new MonthlyViewPlanner(db, a1.id, from, to, now);
        List<TransactionInfo> transactions = planner.getAccountMonthlyView();
        logTransactions(transactions);
        assertTransactions(transactions,
                DateTime.date(2011, 7, 9), 122
        );

    }

    private void prepareData() {
        // regular transactions and transfers
        //t0
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 7, 9).atNoon())
                .account(a1).amount(122).note("t0").create();
        //t1
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 8).atNoon())
                .account(a1).amount(1000).note("t1").create();

        // regular transfer
        //t2
        TransferBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 9).atNoon())
                .fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(50).note("t2").create();

        // regular split
        //t3
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 10).atNoon())
                .account(a1).amount(-500)
                .withSplit(categoriesMap.get("A1"), -200, "t3-s1")
                .withSplit(categoriesMap.get("A1"), -300, "t3-s2")
                .note("t3")
                .create();

        // transfer split
        //t4
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 11).atNoon())
                .account(a1).amount(-100)
                .withTransferSplit(a2, -100, 20, "t4-s1")
                .note("t4")
                .create();
        //t5
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 12).atNoon())
                .account(a2).amount(-120)
                .withSplit(categoriesMap.get("B"), -20, "t5-s1")
                .withTransferSplit(a1, -100, 200, "t5-s2")
                .note("t5")
                .create();

        // payment
        //t6
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 15).atNoon()).account(a1).amount(400).ccPayment().note("t6").create();

        //scheduled once
        //t7
        TransactionBuilder.withDb(db).scheduleOnce(DateTime.date(2011, 8, 14).atNoon()).account(a1).amount(-100).note("t7").create();

        //scheduled recur
        //r1
        TransactionBuilder.withDb(db).scheduleRecur("2011-08-02T21:40:00~DAILY:interval@2#~INDEFINETELY:null")
                .account(a1).amount(-50).note("r1").create();
        //r2
        TransactionBuilder.withDb(db).scheduleRecur("2011-08-02T23:00:00~WEEKLY:days@TUE#interval@1#~INDEFINETELY:null")
                .account(a1).amount(+40).note("r2").create();

        //this should not be included because the account is differ
        TransactionBuilder.withDb(db).scheduleRecur("2011-07-01T21:40:00~DAILY:interval@2#~INDEFINETELY:null")
                .account(a2).amount(-50).note("?").create();

        //these should not be included because the date is out of picture
        TransactionBuilder.withDb(db).scheduleOnce(DateTime.date(2011, 10, 14).at(13, 0, 0, 0))
                .account(a1).amount(-500).note("?").create();
        TransactionBuilder.withDb(db).scheduleRecur("2011-10-01T21:40:00~DAILY:interval@2#~INDEFINETELY:null")
                .account(a1).amount(-500).note("?").create();

        //this is a scheduled transfer which should appear in the monthly view
        //r3
        TransferBuilder.withDb(db).scheduleOnce(DateTime.date(2011, 8, 15).at(13, 0, 0, 0))
                .fromAccount(a1).fromAmount(-210).toAccount(a2).toAmount(51).note("r3").create();
        //r4
        TransferBuilder.withDb(db).scheduleRecur("2011-08-02T21:20:00~WEEKLY:days@FRI#interval@1#~INDEFINETELY:null")
                .fromAccount(a2).fromAmount(-600).toAccount(a1).toAmount(52).note("r3").create();

        //this is a scheduled split with a transfer which should appear in the monthly view
        //r5
        TransactionBuilder.withDb(db).scheduleOnce(DateTime.date(2011, 8, 15).at(14, 0, 0, 0))
                .account(a1).amount(-105)
                .withSplit(categoriesMap.get("A1"), -5, "r5-s1")
                .withTransferSplit(a2, -100, 22, "r5-s2")
                .note("r5")
                .create();
        //r6
        TransactionBuilder.withDb(db).scheduleRecur("2011-08-02T22:30:00~WEEKLY:days@FRI#interval@1#~INDEFINETELY:null")
                .account(a2).amount(-120)
                .withSplit(categoriesMap.get("B"), -20, "r6-s1")
                .withTransferSplit(a1, -88, 30, "r6-s2")
                .note("r6")
                .create();
    }

    private void logTransactions(List<TransactionInfo> transactions) {
        Log.d("MonthlyViewTest", "===== Planned transactions: "+transactions.size()+" =====");
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        for (TransactionInfo transaction : transactions) {
            Log.d("MonthlyViewTest", df.format(new Date(transaction.dateTime))+" "+ Utils.amountToString(Currency.EMPTY, transaction.fromAmount)+" "+transaction.note);
        }
        Log.d("MonthlyViewTest", "==========");
    }

    private void assertTransactions(List<TransactionInfo> transactions, Object...data) {
        int count = data.length/2;
        if (count > transactions.size()) {
            fail("Too few transactions. Expected "+count+", Got "+transactions.size());
        }
        if (count < transactions.size()) {
            fail("Too many transactions. Expected "+count+", Got "+transactions.size());
        }
        for (int i=0; i<count; i++) {
            assertTransaction("Row "+i, transactions.get(i), (DateTime)data[i*2], (Integer)data[i*2+1]);
        }
    }

    private void assertTransaction(String row, TransactionInfo t, DateTime date, long expectedAmount) {
        assertEquals(row, asDate(date), asDate(t.dateTime));
        assertEquals(row, expectedAmount, t.fromAmount);
    }

    private Date asDate(DateTime date) {
        return date.atMidnight().asDate();
    }

    private Date asDate(long date) {
        return asDate(DateTime.fromTimestamp(date));
    }

}
