/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.qif.*;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.orb.Expressions;
import ru.orangesoftware.orb.Query;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static ru.orangesoftware.financisto.export.qif.QifDateFormat.EU_FORMAT;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/17/11 9:19 PM
 */
public class QifImportTest extends AbstractDbTest {

    QifParserTest qifParserTest = new QifParserTest();
    QifImport qifImport;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        db.db().execSQL("insert into currency(_id,title,name,symbol) values(0,'Default','?','$')");
    }

    public void test_should_import_empty_account() throws IOException {
        qifParserTest.test_should_parse_empty_account();
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(1, accounts.size());
        assertEquals("My Cash Account", accounts.get(0).title);
        assertEquals(AccountType.CASH.name(), accounts.get(0).type);
    }

    public void test_should_import_a_couple_of_empty_accounts() throws IOException {
        qifParserTest.test_should_parse_a_couple_of_empty_accounts();
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        sortAccountsById(accounts);

        assertEquals(2, accounts.size());
        assertEquals("My Cash Account", accounts.get(0).title);
        assertEquals(AccountType.CASH.name(), accounts.get(0).type);
        assertEquals("My Bank Account", accounts.get(1).title);
        assertEquals(AccountType.BANK.name(), accounts.get(1).type);
    }

    public void test_should_import_categories() throws Exception {
        QifParser p = new QifParser(null, QifDateFormat.EU_FORMAT);
        p.categories.add(new QifCategory("P1:c1", true));
        p.categories.add(new QifCategory("P1:c2", true));
        p.categories.add(new QifCategory("P2", false));
        p.categories.add(new QifCategory("P2:c1", false));
        p.categories.add(new QifCategory("P1", false));
        doImport(p);

        CategoryTree<Category> categories = db.getCategoriesTree(false);
        assertNotNull(categories);
        assertEquals(2, categories.size());

        Category c = categories.getAt(0);
        assertCategory("P1", true, c);
        assertEquals(2, c.children.size());
        assertCategory("c1", true, c.children.getAt(0));
        assertCategory("c2", true, c.children.getAt(1));
        assertNotNull(qifImport.findCategory("P1"));
        assertNotNull(qifImport.findCategory("P1:c1"));
        assertNotNull(qifImport.findCategory("P1:c2"));

        c = categories.getAt(1);
        assertCategory("P2", false, c);
        assertEquals(1, c.children.size());
        assertCategory("c1", false, c.children.getAt(0));
        assertNotNull(qifImport.findCategory("P2:c1"));
    }

    private void assertCategory(String name, boolean isIncome, Category c) {
        assertEquals(name, c.title);
        assertEquals(isIncome, c.isIncome());
    }

    public void test_should_import_account_with_a_couple_of_transactions() throws Exception {
        qifParserTest.test_should_parse_account_with_a_couple_of_transactions();
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(1, accounts.size());

        List<TransactionInfo> transactions = em.getTransactionsForAccount(accounts.get(0).id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals(1000, t.fromAmount);
        assertEquals("P1", t.category.title);
        assertNull(t.payee);

        t = transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals(-2056, t.fromAmount);
        assertEquals("Payee 1", t.payee.title);
        assertEquals("c1", t.category.title);
        assertEquals("Some note here...", t.note);
    }

    public void test_should_import_multiple_accounts() throws Exception {
        qifParserTest.test_should_parse_multiple_accounts();
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals(-2000, t.fromAmount);

        t = transactions.get(1);
        assertEquals(DateTime.date(2011, 1, 2).atMidnight().asLong(), t.dateTime);
        assertEquals(5400, t.fromAmount);

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = em.getTransactionsForAccount(a.id);
        assertEquals(3, transactions.size());

        t = transactions.get(0);
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals(1000, t.fromAmount);

        t = transactions.get(1);
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals(-2345, t.fromAmount);

        t = transactions.get(2);
        assertEquals(DateTime.date(2011, 1, 1).atMidnight().asLong(), t.dateTime);
        assertEquals(-6780, t.fromAmount);
    }

    public void test_should_import_transfers() throws Exception {
        qifParserTest.test_should_parse_transfers();
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertTrue("Should be a transfer from bank to cash", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-2000, t.fromAmount);
        assertEquals("My Cash Account", t.toAccount.title);
        assertEquals(2000, t.toAmount);

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = em.getTransactionsForAccount(a.id);
        assertEquals(0, transactions.size());
    }

    public void test_should_import_convert_unknown_transfers_into_regular_transactions_with_a_special_note() throws Exception {
        qifParserTest.parseQif(
            "!Account\n" +
            "NMy Cash Account\n" +
            "TCash\n" +
            "^\n" +
            "!Type:Cash\n" +
            "D08/02/2011\n" +
            "T25.00\n" +
            "L[My Bank Account]\n" +
            "^\n" +
            "D07/02/2011\n" +
            "T55.00\n" +
            "L[Some Account 1]\n" +
            "^\n" +
            "!Account\n" +
            "NMy Bank Account\n" +
            "TBank\n" +
            "^\n" +
            "!Type:Bank\n" +
            "D08/02/2011\n" +
            "T-20.00\n" +
            "MNote on transfer\n" +
            "L[Some Account 2]\n" +
            "^\n"+
            "D07/02/2011\n" +
            "T-30.00\n" +
            "L[My Cash Account]\n" +
            "^\n");
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-2000, t.fromAmount);
        assertEquals("Transfer: Some Account 2 | Note on transfer", t.note);

        t = transactions.get(1);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals("My Bank Account", t.fromAccount.title);
        assertEquals(-3000, t.fromAmount);
        assertEquals("Transfer: My Cash Account", t.note);

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = em.getTransactionsForAccount(a.id);
        assertEquals(2, transactions.size());

        t = transactions.get(0);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 8).atMidnight().asLong(), t.dateTime);
        assertEquals("My Cash Account", t.fromAccount.title);
        assertEquals(2500, t.fromAmount);
        assertEquals("Transfer: My Bank Account", t.note);

        t = transactions.get(1);
        assertFalse("Should not be a transfer", t.isTransfer());
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asLong(), t.dateTime);
        assertEquals("My Cash Account", t.fromAccount.title);
        assertEquals(5500, t.fromAmount);
        assertEquals("Transfer: Some Account 1", t.note);
    }

    public void test_should_import_splits() throws Exception {
        qifParserTest.test_should_parse_splits();
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(1, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(-260066, t.fromAmount);

        List<TransactionInfo> splits = getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());

        TransactionInfo s = splits.get(0);
        assertEquals("A1", s.category.title);
        assertEquals(-110056, s.fromAmount);
        assertEquals("Note on first split", s.note);

        s = splits.get(1);
        assertEquals("A2", s.category.title);
        assertEquals(-100000, s.fromAmount);

        s = splits.get(2);
        assertEquals("<NO_CATEGORY>", s.category.title);
        assertEquals(50010, s.fromAmount);
        assertEquals("Note on third split", s.note);
    }

    public void test_should_import_transfer_splits() throws Exception {
        qifParserTest.test_should_parse_transfer_splits();
        doImport();

        List<Account> accounts = em.getAllAccountsList();
        assertEquals(2, accounts.size());

        Account a = accounts.get(0);
        assertEquals("My Bank Account", a.title);
        assertEquals(AccountType.BANK.name(), a.type);

        List<TransactionInfo> transactions = em.getTransactionsForAccount(a.id);
        assertEquals(0, transactions.size());

        a = accounts.get(1);
        assertEquals("My Cash Account", a.title);
        assertEquals(AccountType.CASH.name(), a.type);

        transactions = em.getTransactionsForAccount(a.id);
        assertEquals(1, transactions.size());

        TransactionInfo t = transactions.get(0);
        assertEquals(-210000, t.fromAmount);

        List<TransactionInfo> splits = getSplitsForTransaction(t.id);
        assertEquals(2, splits.size());

        TransactionInfo s = splits.get(0);
        assertEquals("A1", s.category.title);
        assertEquals(-110000, s.fromAmount);
        assertEquals("Note on first split", s.note);

        s = splits.get(1);
        assertTrue(s.isTransfer());
        assertEquals("My Bank Account", s.toAccount.title);
        assertEquals(-100000, s.fromAmount);
        assertEquals(100000, s.toAmount);
    }

    private List<TransactionInfo> getSplitsForTransaction(long transactionId) {
        Query<TransactionInfo> q = em.createQuery(TransactionInfo.class);
        q.where(Expressions.eq("parentId", transactionId));
        return q.list();
    }

    private void sortAccountsById(List<Account> accounts) {
        Collections.sort(accounts, new Comparator<Account>() {
            @Override
            public int compare(Account a1, Account a2) {
                return a1.id == a2.id ? 0 : (a1.id > a2.id ? 1 : -1);
            }
        });
    }

    private void doImport() {
        doImport(qifParserTest.p);
    }

    private void doImport(QifParser p) {
        QifImportOptions options = new QifImportOptions("", EU_FORMAT, Currency.EMPTY);
        qifImport = new QifImport(getContext(), db, options);
        qifImport.doImport(p);
    }


}