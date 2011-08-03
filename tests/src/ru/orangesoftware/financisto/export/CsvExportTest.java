/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.export.csv.CsvExport;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.test.*;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/3/11 12:04 AM
 */
public class CsvExportTest extends AbstractExportTest<CsvExport, CsvExportOptions> {

    Account a1;
    Account a2;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = createFirstAccount();
        a2 = createSecondAccount();
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
        CurrencyCache.initialize(db.em());
    }

    public void test_should_include_header() throws Exception {
        CsvExportOptions options = new CsvExportOptions(Currency.EMPTY, ',', true, WhereFilter.empty(), false);
        assertEquals("date,time,account,amount,currency,category,parent,payee,location,project,note\n", exportAsString(options));
    }

    public void test_should_export_regular_transaction() throws Exception {
        CsvExportOptions options = new CsvExportOptions(createExportCurrency(), ',', false, WhereFilter.empty(), false);
        TransactionBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 34, 55, 10))
                .account(a1).amount(-123456).category(categoriesMap.get("AA1")).payee("P1").location("Home").project("P1").note("My note").create();
        assertEquals("2011-08-03,22:34:55,My Cash Account,-1234.56,SGD,AA1,A:A1,P1,Home,P1,My note\n", exportAsString(options));
    }

    public void test_should_export_regular_transfer() throws Exception {
        CsvExportOptions options = new CsvExportOptions(createExportCurrency(), ',', false, WhereFilter.empty(), false);
        TransferBuilder.withDb(db).dateTime(DateTime.date(2011, 8, 3).at(22, 46, 0, 0))
                .fromAccount(a1).fromAmount(-450000).toAccount(a2).toAmount(25600).create();
        assertEquals(
                "2011-08-03,22:46:00,My Cash Account,-4500.00,SGD,\"\",\"\",\"\",Transfer Out,<NO_PROJECT>,\n"+
                "2011-08-03,22:46:00,My Bank Account,256.00,CZK,\"\",\"\",\"\",Transfer In,<NO_PROJECT>,\n",
                exportAsString(options));
    }

    private Currency createExportCurrency() {
        Currency c = CurrencyBuilder.withDb(db)
                .title("USD")
                .name("USD")
                .symbol("$")
                .separators("''", "'.'")
                .create();
        assertNotNull(em.load(Currency.class, c.id));
        return c;
    }

    @Override
    protected CsvExport createExport(CsvExportOptions options) {
        return new CsvExport(db, options);
    }

}
