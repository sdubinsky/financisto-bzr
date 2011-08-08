/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import android.database.DatabaseUtils;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.test.*;

import java.util.Date;
import java.util.Map;

public class MonthlyViewTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        a2 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
    }

    public void test_should_generate_monthly_view_for_account() {
        // regular transactions and transfers
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 8)).account(a1).amount(1000).create();

        // regular transfer
        TransferBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 9))
                .fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(50).create();

        // regular split
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 10))
                .account(a1).amount(-500)
                .withSplit(categoriesMap.get("A1"), -200)
                .withSplit(categoriesMap.get("A1"), -300)
                .create();

        // transfer split
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 11))
                .account(a1).amount(-100)
                .withTransferSplit(a2, -100, 20)
                .create();
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 12))
                .account(a2).amount(-120)
                .withSplit(categoriesMap.get("B"), -20)
                .withTransferSplit(a1, -100, 200)
                .create();

        // payment
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 15)).account(a1).amount(400).ccPayment().create();

        //a1
        //2011-08-08 +1000
        //2011-08-09 -100 -> a2
        //2011-08-10 -500
        //2011-08-11 -100 -> a2
        //2011-08-12 +200 <- a2
        //2011-08-15 +400 (payment)
        long from = DateTime.date(2011, 8, 8).atMidnight().asLong();
        long to = DateTime.date(2011, 8, 31).at(23, 59, 59, 999).asLong();

        Cursor c  = db.getAccountMonthlyView(a1.id, from, to);
        assertTransactions(c,
                DateTime.date(2011, 8, 8), 1000,
                DateTime.date(2011, 8, 9), -100,
                DateTime.date(2011, 8, 10), -500,
                DateTime.date(2011, 8, 11), -100,
                DateTime.date(2011, 8, 12), 200,
                DateTime.date(2011, 8, 15), 400
        );

        c = db.getMonthlyViewExpenses(a1.id, from, to);
        assertTransactions(c,
                DateTime.date(2011, 8, 9), -100,
                DateTime.date(2011, 8, 10), -500,
                DateTime.date(2011, 8, 11), -100
        );

        c = db.getMonthlyViewCredits(a1.id, from, to);
        assertTransactions(c,
                DateTime.date(2011, 8, 8), 1000,
                DateTime.date(2011, 8, 12), 200
        );

        c = db.getMonthlyViewPayments(a1.id, from, to);
        assertTransactions(c,
                DateTime.date(2011, 8, 15), 400
        );
    }

    private void assertTransactions(Cursor c, Object...data) {
        try {
            DatabaseUtils.dumpCursor(c);
            int count = data.length/2;
            for (int i=0; i<count; i++) {
                assertTransaction("Row "+i, c, (DateTime)data[i*2], (Integer)data[i*2+1]);
            }
            assertEndOfReport(c);
        } finally {
            c.close();
        }
    }

    private void assertTransaction(String row, Cursor c, DateTime date, long expectedAmount) {
        if (c.moveToNext()) {
            long dateTime = c.getLong(DatabaseHelper.BlotterColumns.datetime.ordinal());
            long amount = c.getLong(DatabaseHelper.BlotterColumns.from_amount.ordinal());
            assertEquals(row, asDate(date), asDate(dateTime));
            assertEquals(row, expectedAmount, amount);
            return;
        }
        fail("No more transactions");
    }

    private void assertEndOfReport(Cursor c) {
        if (c.moveToNext()) {
            fail("Too many transactions");
        }
    }

    private Date asDate(DateTime date) {
        return date.atMidnight().asDate();
    }

    private Date asDate(long date) {
        return asDate(DateTime.fromTimestamp(date));
    }

}
