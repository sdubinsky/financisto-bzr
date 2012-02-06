/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.test.*;

import java.util.Map;

import static ru.orangesoftware.financisto.db.DatabaseAdapter.enhanceFilterForAccountBlotter;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/31/12 8:19 PM
 */
public class TransactionsTotalCalculatorTest extends AbstractDbTest {

    Currency c1;
    Currency c2;
    Currency c3;

    Account a1;
    Account a2;

    TransactionsTotalCalculator c;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);

        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
        c3 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").create();

        c = new TransactionsTotalCalculator(db, enhanceFilterForAccountBlotter(WhereFilter.empty()));

        a1 = AccountBuilder.withDb(db).title("Cash").currency(c1).create();
        a2 = AccountBuilder.withDb(db).title("Bank").currency(c2).create();

        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78635f).create();

        RateBuilder.withDb(db).from(c1).to(c3).at(DateTime.date(2012, 1, 5)).rate(0.62510f).create();
        RateBuilder.withDb(db).from(c2).to(c3).at(DateTime.date(2012, 1, 5)).rate(0.12453f).create();

        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 10)).amount(1).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 17).at(13, 30, 0, 0)).amount(100).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 22).atMidnight()).amount(-450).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 23)).amount(-200)
                .withSplit(categories.get("A1"), -50)
                .withTransferSplit(a2, -150, 100)
                .create();

        TransactionBuilder.withDb(db).account(a2).dateTime(DateTime.date(2012, 1, 18).at(18, 40, 0, 0)).amount(-250).create();
        TransactionBuilder.withDb(db).account(a2).dateTime(DateTime.date(2012, 1, 17).at(13, 30, 0, 0)).amount(-100).create();

        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).dateTime(DateTime.date(2012, 1, 20).atNoon()).fromAmount(-50).toAmount(20).create();
    }

    public void test_should_calculate_blotter_total_in_multiple_currencies() {
        Total[] totals = c.getTransactionsBalance();
        assertEquals(2, totals.length);
        assertEquals(-599, totals[0].balance);
        assertEquals(-230, totals[1].balance);
    }

    public void test_should_calculate_blotter_total_in_home_currency() {
        assertEquals((long)(1f +100f -450f -200f -(1f/0.78635f)*250f -(1f/0.78592f)*100f), c.getTransactionsBalance(c1));
        assertEquals((long)(1f +0.78592f*100f -0.78635f*450f -0.78635f*200f -250f -100f), c.getTransactionsBalance(c2));
    }

    public void test_should_calculate_account_total_in_home_currency() {
        //no conversion
        assertEquals((long)(1f +100f -450f -200f -50f), c.getTransactionsBalance(a1, c1));

        //note that the last amount is taken from the transfer without conversion
        assertEquals((long)(1f +0.78592f*100f -0.78635f*450f -0.78635f*200f -20f), c.getTransactionsBalance(a1, c2));

        //no conversion
        assertEquals((long)(-250f -100f +20f +100f), c.getTransactionsBalance(a2, c2));

        //conversion+transfers
        assertEquals((long)(-(1f/0.78635f)*250f -(1f/0.78592f)*100f +50f +150f), c.getTransactionsBalance(a2, c1));

        //conversions
        assertEquals((long)(0.62510f*(1f +100f -450f -200f -50f)), c.getTransactionsBalance(a1, c3));
        assertEquals((long)(0.12453f*(-250f -100f +20f +100f)), c.getTransactionsBalance(a2, c3));
    }

    public void test_should_detect_multiple_account_currencies() {
        // one account only
        assertTrue(db.singleCurrencyOnly());

        // two accounts with the same currency
        AccountBuilder.withDb(db).currency(a1.currency).title("Account2").create();
        assertTrue(db.singleCurrencyOnly());

        //another account with a different currency, but not included into totals
        Currency c2 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        AccountBuilder.withDb(db).currency(c2).title("Account3").doNotIncludeIntoTotals().create();
        assertTrue(db.singleCurrencyOnly());

        //this account is not active
        AccountBuilder.withDb(db).currency(c2).title("Account4").inactive().create();
        assertTrue(db.singleCurrencyOnly());

        //now it's two currencies
        AccountBuilder.withDb(db).currency(c2).title("Account5").create();
        assertFalse(db.singleCurrencyOnly());
    }

}
