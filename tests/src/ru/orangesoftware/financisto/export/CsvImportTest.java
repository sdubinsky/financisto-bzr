/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import android.util.Log;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.export.csv.CsvImport;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 12/6/11 11:31 PM
 *
 * Default format described here
 * https://docs.google.com/spreadsheet/ccc?key=0AiE-9LlEldfYdFMzVHUtenktTkhoN1dMd1FaOUJaY1E
 */
public class CsvImportTest extends AbstractImportExportTest {

    Map<String, Category> categories;
    CsvImport csvImport;
    CsvImportOptions defaultOptions;
    long defaultAccountId;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        categories = CategoryBuilder.createDefaultHierarchy(db);
        defaultOptions = createDefaultOptions();
        defaultAccountId = defaultOptions.selectedAccountId;
    }

    public void test_should_import_empty_file() throws Exception {
        doImport("", defaultOptions);
    }

    public void test_should_import_one_transaction_into_the_selected_account() throws Exception {
        doImport("date,time,account,amount,currency,category,parent,payee,location,project,note\n" +
                "10.07.2011,07:13:17,AAA,-10.50,SGD,AA1,A:A1,P1,,,", defaultOptions);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(defaultAccountId);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 7, 10).at(7, 13, 17, 0).asLong(), t.dateTime);
        assertEquals(defaultAccountId, t.fromAccount.id);
        assertEquals(-1050, t.fromAmount);
        assertEquals(categories.get("AA1").id, t.category.id);
        assertEquals("P1", t.payee.title);
    }

    public void test_should_import_one_transaction_without_the_header() throws Exception {
        defaultOptions.useHeaderFromFile = false;
        doImport("10.07.2011,07:13:17,AAA,2100.56,SGD,A1,\"\",P1,Current location,No project,", defaultOptions);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(defaultAccountId);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 7, 10).at(7, 13, 17, 0).asLong(), t.dateTime);
        assertEquals(defaultAccountId, t.fromAccount.id);
        assertEquals(210056, t.fromAmount);
        assertEquals(categories.get("A1").id, t.category.id);
        assertEquals("P1", t.payee.title);
    }

    public void doImport(String csv, CsvImportOptions options) throws Exception {
        File tmp = File.createTempFile("backup", ".csv");
        FileWriter w = new FileWriter(tmp);
        w.write(csv);
        w.close();
        Log.d("Financisto", "Created a temporary backup file: " + tmp.getAbsolutePath());
        options = new CsvImportOptions(options.currency, options.dateFormat.toPattern(),
                options.selectedAccountId, options.filter, tmp.getAbsolutePath(), options.fieldSeparator, options.useHeaderFromFile);
        csvImport = new CsvImport(db, options);
        csvImport.doImport();
    }

    public CsvImportOptions createDefaultOptions() {
        Account a = createFirstAccount();
        Currency c = a.currency;
        return new CsvImportOptions(c, CsvImportOptions.DEFAULT_DATE_FORMAT, a.id, WhereFilter.empty(), null, ',', true);
    }

}
