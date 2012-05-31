/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.*;

import java.util.Map;

import static ru.orangesoftware.financisto.test.DateTime.date;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 5/25/12 11:26 PM
 */
public class AccountPurgeTest extends AbstractDbTest {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        a2 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
        /*                        A1     A2
        * 29/05 A1 +10          | 10   |
        * 28/05 A1 -20          | -10  |
        * 27/05 A1->A2 -100 +20 | -110 | 20
        * 26/05 A1 +100         | -10  |
        * 25/05 A2->A1 -50 +10  |  0   | -30
        * 24/05 A1 +200         | 200  |
        * 24/05 A2 -20          |      | -50
        * 23/05 A1 -150         | 50   |
        *          -100         |      |
        *    -> A2 -50 +10      |      | -40
        * 22/05 A1 -20          | 30   |
        * 21/05 A1 +10          | 40   |
        * */
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 29)).account(a1).amount(10).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 28)).account(a1).amount(-20).create();
        TransferBuilder.withDb(db).dateTime(date(2012, 5, 27))
                .fromAccount(a1).fromAmount(-100)
                .toAccount(a2).toAmount(20).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 26)).account(a1).amount(100).create();
        TransferBuilder.withDb(db).dateTime(date(2012, 5, 25))
                .fromAccount(a2).fromAmount(-50)
                .toAccount(a1).toAmount(10).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 24)).account(a1).amount(200).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 24)).account(a2).amount(-20).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 23)).account(a1).amount(-150)
                .withSplit(categoriesMap.get("A1"), -100)
                .withTransferSplit(a2, -50, 10)
                .create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 22)).account(a1).amount(-20).create();
        TransactionBuilder.withDb(db).dateTime(date(2012, 5, 21)).account(a1).amount(10).create();
    }

    public void test_should_purge_old_transactions_older_than_specified_date() {
        //given
        assertAccounts();
        assertTransactionsCount(a1, 10);
        assertTransactionsCount(a2, 2);
        assertOldestTransaction(a1, date(2012, 5, 21), 10);
        //when
        db.purgeAccountAtDate(a1, date(2012, 5, 20).asLong());
        assertOldestTransaction(a1, date(2012, 5, 21), 10);
        assertAccounts();
        //when
        db.purgeAccountAtDate(a1, date(2012, 5, 21).asLong());
        assertOldestTransaction(a1, date(2012, 5, 21).atDayEnd(), 10);
        assertAccounts();
    }

    private void assertOldestTransaction(Account account, DateTime date, long expectedAmount) {
        Transaction t = getOldestTransaction(account);
        assertEquals(date.asLong(), t.dateTime);
        assertEquals(expectedAmount, t.fromAmount);
        // this is the very first transaction, so running balance == amount
        assertAccountBalanceForTransaction(t, account, expectedAmount);
    }

    private Transaction getOldestTransaction(Account account) {
        long id = DatabaseUtils.rawFetchId(db,
                "select _id from transactions where from_account_id=? and is_template=0 order by datetime limit 1",
                new String[]{String.valueOf(account.id)});
        return em.get(Transaction.class, id);
    }

    private void assertAccounts() {
        assertAccount(a1, 40);
        assertAccount(a2, -40);
    }

    private void assertAccount(Account account, long accountTotal) {
        assertAccountTotal(account, accountTotal);
        assertFinalBalanceForAccount(account, accountTotal);
    }

}
