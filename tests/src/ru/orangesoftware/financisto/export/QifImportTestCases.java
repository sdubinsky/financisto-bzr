/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.qif.QifImport;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.test.DateTime;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/17/11 9:19 PM
 */
public class QifImportTestCases extends AbstractDbTest {

    QifParserTest qifParserTest = new QifParserTest();
    QifImport qifImport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        db.db().execSQL("insert into currency(_id,title,name,symbol) values(0,'Default','?','$')");
    }

    public void test_should_import_homebank_case_1() throws Exception {
        doImport("!Account\n" +
                "NMy Bank Account\n" +
                "^\n" +
                "!Type:Bank\n" +
                "D02/11/2011\n" +
                "T14.00\n" +
                "C\n" +
                "PP2\n" +
                "M(null)\n" +
                "LC2\n" +
                "^\n" +
                "D01/11/2011\n" +
                "T-35.40\n" +
                "C\n" +
                "P\n" +
                "M(null)\n" +
                "L[My Cash Account]\n" +
                "^\n" +
                "!Account\n" +
                "NMy Cash Account\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D03/11/2011\n" +
                "T19.50\n" +
                "C\n" +
                "PP1\n" +
                "M(null)\n" +
                "LC1:c1\n" +
                "^\n" +
                "D01/11/2011\n" +
                "T35.40\n" +
                "C\n" +
                "P\n" +
                "M(null)\n" +
                "L[My Bank Account]\n" +
                "^");

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 11, 2).atMidnight().asLong(), t.dateTime);
        assertEquals(1400, t.fromAmount);
        assertEquals("P2", t.payee.title);
        assertEquals("C2", t.category.title);

        t = transactions.get(1);
        assertTrue("Should be a transfer from bank to cash", t.isTransfer());
        assertEquals(DateTime.date(2011, 11, 1).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-3540, t.fromAmount);
        assertEquals("My Cash Account", t.toAccount.title);
        assertEquals(3540, t.toAmount);

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = em.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        t = transactions.get(0);
        assertEquals(DateTime.date(2011, 11, 3).atMidnight().asLong(), t.dateTime);
        assertEquals(1950, t.fromAmount);
        assertEquals("P1", t.payee.title);
        assertEquals("c1", t.category.title);
    }

    private void sortAccountsById(List<Account> accounts) {
        Collections.sort(accounts, new Comparator<Account>() {
            @Override
            public int compare(Account a1, Account a2) {
                return a1.id == a2.id ? 0 : (a1.id > a2.id ? 1 : -1);
            }
        });
    }

    private void doImport(String qif) throws IOException {
        qifParserTest.parseQif(qif);
        QifImportOptions options = new QifImportOptions("", "", Currency.EMPTY);
        qifImport = new QifImport(db, options);
        qifImport.doImport(qifParserTest.p);
    }

}
