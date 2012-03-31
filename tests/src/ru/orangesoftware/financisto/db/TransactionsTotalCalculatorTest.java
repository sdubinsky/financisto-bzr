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

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/31/12 8:19 PM
 */
public class TransactionsTotalCalculatorTest extends AbstractDbTest {

    Currency c1, c2, c3;
    Account a1, a2, a3;

    TransactionsTotalCalculator c;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);

        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").makeDefault().create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
        c3 = CurrencyBuilder.withDb(db).name("SGD").title("Singapore Dollar").symbol("S$").create();

        c = new TransactionsTotalCalculator(db, WhereFilter.empty());

        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 17)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78635f).create();

        RateBuilder.withDb(db).from(c1).to(c3).at(DateTime.date(2012, 1, 5)).rate(0.62510f).create();
        RateBuilder.withDb(db).from(c2).to(c3).at(DateTime.date(2012, 1, 5)).rate(0.12453f).create();

        a1 = AccountBuilder.withDb(db).title("Cash").currency(c1).create();
        a2 = AccountBuilder.withDb(db).title("Bank").currency(c2).create();
        a3 = AccountBuilder.withDb(db).title("Cash2").currency(c1).doNotIncludeIntoTotals().create();

        /*
        10 A3 SGD +555
        10 A1 USD +1
        17 A1 USD +100
        17 A2 EUR -100
        18 A2 EUR -250
        20 A1 USD -50 FT
        20 A2 EUR +20 TT
        22 A1 USD -450
        23 A1 USD -50      S
        23 A1 USD -150 FT  S
        23 A2 EUR +100 TT  S
         */

        TransactionBuilder.withDb(db).account(a3).dateTime(DateTime.date(2012, 1, 10)).amount(555).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 10)).amount(1).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 17).at(13, 30, 0, 0)).amount(100).create();
        TransactionBuilder.withDb(db).account(a2).dateTime(DateTime.date(2012, 1, 17).at(13, 30, 0, 0)).amount(-100).create();
        TransactionBuilder.withDb(db).account(a2).dateTime(DateTime.date(2012, 1, 18).at(18, 40, 0, 0)).amount(-250).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).dateTime(DateTime.date(2012, 1, 20).atNoon()).fromAmount(-50).toAmount(20).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 22).atMidnight()).amount(-450).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.date(2012, 1, 23)).amount(-200)
                .withSplit(categories.get("A1"), -50)
                .withTransferSplit(a2, -150, 100)
                .create();
    }

    public void test_should_calculate_blotter_total_in_multiple_currencies() {
        Total[] totals = c.getTransactionsBalance();
        assertEquals(2, totals.length);
        assertEquals(-599, totals[0].balance);
        assertEquals(-230, totals[1].balance);
    }

    public void test_should_calculate_blotter_total_in_home_currency() {
        assertEquals((long)(1f +100f -(1f/0.78592f)*100f -(1f/0.78635f)*250f -50f +50f -450f -50f -150f +150f), c.getBlotterBalance(c1));
        assertEquals((long)(1f +0.78592f*100f -100f -250f -20f +20f -0.78635f*450f -0.78635f*50f -100f +100f), c.getBlotterBalance(c2));
        assertEquals(c.getBlotterBalance(c1), c.getBlotterBalanceInHomeCurrency().balance);
    }

    public void test_should_calculate_account_total_in_home_currency() {
        //no conversion
        assertEquals((long) (1f + 100f -50f -450f -50f -150f), c.getAccountBalance(c1, a1.id));

        //note that the last amount is taken from the transfer without conversion
        assertEquals((long) (1f + 0.78592f * 100f - 0.78635f * 450f - 0.78635f * 200f - 20f), c.getAccountBalance(c2, a1.id));

        //no conversion
        assertEquals((long)(-250f -100f +20f +100f), c.getAccountBalance(c2, a2.id));

        //conversion+transfers
        assertEquals((long) (-(1f / 0.78635f) * 250f - (1f / 0.78592f) * 100f + 50f + 150f), c.getAccountBalance(c1, a2.id));

        //conversions
        assertEquals((long) (0.62510f * (1f + 100f - 450f - 200f - 50f)), c.getAccountBalance(c3, a1.id));
        assertEquals((long) (0.12453f * (-250f - 100f + 20f + 100f)), c.getAccountBalance(c3, a2.id));
    }

}
