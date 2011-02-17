package ru.orangesoftware.financisto.export;

import android.util.Log;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.qif.QifExport;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.test.MockDateTime;
import ru.orangesoftware.financisto.test.MockTransaction;
import ru.orangesoftware.financisto.test.MockTransfer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/4/11 10:23 PM
 */
public class QIFExportTest extends AbstractDbTest {

    public void test_should_export_empty_file() throws Exception {
        String output = exportAsString();
        assertNotNull(output);
        assertTrue(output.length() == 0);
    }

    public void test_should_export_empty_account() throws Exception {
        createFirstAccount();
        assertEquals(
                "!Account\n" +
                        "NMy Cash Account\n" +
                        "TCash\n" +
                        "^\n",
                exportAsString());
    }

    public void test_should_export_account_with_a_couple_of_transactions() throws Exception {
        Account a = createFirstAccount();
        Category p1 = createExpenseCategory("P1");
        Category c1 = createCategory(p1, "c1");
        MockTransaction.withDb(db).account(a).amount(1000).category(p1).dateTime(MockDateTime.on().year(2011).feb().day(8)).create();
        MockTransaction.withDb(db).account(a).amount(-2056).category(c1).payee("Payee 1").note("Some note here...").dateTime(MockDateTime.on().year(2011).feb().day(7)).create();
        assertEquals(
                "!Type:Cat\n"+
                "NP1\n"+
                "E\n"+
                "^\n"+
                "NP1:c1\n"+
                "E\n"+
                "^\n"+
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n"+
                "!Type:Cash\n"+
                "D08/02/2011\n"+
                "T10.00\n"+
                "LP1\n"+
                "^\n"+
                "D07/02/2011\n"+
                "T-20.56\n"+
                "LP1:c1\n"+
                "PPayee 1\n"+
                "MSome note here...\n"+
                "^\n",
                exportAsString());
    }

    public void test_should_export_multiple_accounts() throws Exception {
        createSampleData();
        assertEquals(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D08/02/2011\n" +
                "T10.00\n" +
                "^\n" +
                "D07/02/2011\n" +
                "T-23.45\n" +
                "^\n" +
                "D01/01/2011\n" +
                "T-67.80\n" +
                "^\n" +
                "!Account\n" +
                "NMy Bank Account\n" +
                "TBank\n" +
                "^\n" +
                "!Type:Bank\n" +
                "D08/02/2011\n" +
                "T-20.00\n" +
                "^\n" +
                "D02/01/2011\n" +
                "T54.00\n" +
                "^\n",
                exportAsString());
    }

    public void test_should_export_only_transactions_in_the_specified_range() throws Exception {
        createSampleData();
        WhereFilter filter = createFebruaryOnlyFilter();
        assertEquals(
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n"+
                "!Type:Cash\n"+
                "D08/02/2011\n"+
                "T10.00\n"+
                "^\n"+
                "D07/02/2011\n"+
                "T-23.45\n"+
                "^\n"+
                "!Account\n"+
                "NMy Bank Account\n"+
                "TBank\n"+
                "^\n"+
                "!Type:Bank\n"+
                "D08/02/2011\n"+
                "T-20.00\n"+
                "^\n",
                exportAsString(filter));
    }

    public void test_should_export_transfers() throws Exception {
        createTransfers();
        assertEquals(
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n"+
                "!Type:Cash\n"+
                "D08/02/2011\n"+
                "T20.00\n"+
                "L[My Bank Account]\n"+
                "^\n"+
                "!Account\n"+
                "NMy Bank Account\n"+
                "TBank\n"+
                "^\n"+
                "!Type:Bank\n"+
                "D08/02/2011\n"+
                "T-20.00\n"+
                "L[My Cash Account]\n"+
                "^\n",
                exportAsString());
    }

    private void createTransfers() {
        Account a1 = createFirstAccount();
        Account a2 = createSecondAccount();
        MockTransfer.withDb(db).fromAccount(a2).fromAmount(-2000).toAccount(a1).toAmount(2000).dateTime(MockDateTime.on().year(2011).feb().day(8)).create();
    }

    public void test_should_export_categories() throws Exception {
        createCategories();
        assertEquals(
                "!Type:Cat\n"+
                "NA1\n"+
                "I\n"+
                "^\n"+
                "NA1:aa1\n"+
                "I\n"+
                "^\n"+
                "NA1:aa2\n"+
                "I\n"+
                "^\n"+
                "NB2\n"+
                "E\n"+
                "^\n"+
                "NB2:bb1\n"+
                "E\n"+
                "^\n",
                exportAsString());
    }

    private void createCategories() {
        Category a1 = createIncomeCategory("A1");
        createCategory(a1, "aa1");
        createCategory(a1, "aa2");
        Category b1 = createExpenseCategory("B2");
        createCategory(b1, "bb1");
    }

    private WhereFilter createFebruaryOnlyFilter() {
        String start = String.valueOf(MockDateTime.on().year(2011).feb().day(1).atMidnight().asLong());
        String end = String.valueOf(MockDateTime.on().year(2011).feb().day(28).atDayEnd().asLong());
        return WhereFilter.empty().btw(BlotterFilter.DATETIME, start, end);
    }

    private void createSampleData() {
        Account a1 = createFirstAccount();
        MockTransaction.withDb(db).account(a1).amount(1000).dateTime(MockDateTime.on().year(2011).feb().day(8)).create();
        MockTransaction.withDb(db).account(a1).amount(-2345).dateTime(MockDateTime.on().year(2011).feb().day(7)).create();
        MockTransaction.withDb(db).account(a1).amount(-6780).dateTime(MockDateTime.on().year(2011).jan().day(1).atNoon()).create();
        Account a2 = createSecondAccount();
        MockTransaction.withDb(db).account(a2).amount(-2000).dateTime(MockDateTime.on().year(2011).feb().day(8)).create();
        MockTransaction.withDb(db).account(a2).amount(5400).dateTime(MockDateTime.on().year(2011).jan().day(2).atMidnight()).create();
    }

    private Account createFirstAccount() {
        Currency c = createCurrency();
        Account a = new Account();
        a.title = "My Cash Account";
        a.type = AccountType.CASH.name();
        a.currency = c;
        a.totalAmount = 10000;
        a.sortOrder = 100;
        em.saveAccount(a);
        assertNotNull(em.load(Account.class, a.id));
        return a;
    }

    private Account createSecondAccount() {
        Currency c = em.load(Currency.class, 1);
        Account a = new Account();
        a.title = "My Bank Account";
        a.type = AccountType.BANK.name();
        a.currency = c;
        a.totalAmount = 23450;
        a.sortOrder = 50;
        em.saveAccount(a);
        assertNotNull(em.load(Account.class, a.id));
        return a;
    }

    private Currency createCurrency() {
        Currency c = new Currency();
        c.title = "Singapore Dollar";
        c.name = "SGD";
        c.symbol = "S$";
        em.saveOrUpdate(c);
        assertNotNull(em.load(Currency.class, c.id));
        return c;
    }

    private Category createExpenseCategory(String name) {
        return createCategory(name, Category.TYPE_EXPENSE);
    }

    private Category createIncomeCategory(String name) {
        return createCategory(name, Category.TYPE_INCOME);
    }

    private Category createCategory(String name, int type) {
        Category c = new Category();
        c.title = name;
        c.type = type;
        c.id = db.insertOrUpdate(c, new ArrayList<Attribute>());
        return c;
    }

    private Category createCategory(Category parent, String name) {
        Category c = new Category();
        c.title = name;
        c.parent = parent;
        c.id = db.insertOrUpdate(c, new ArrayList<Attribute>());
        return c;
    }

    private String exportAsString() throws Exception {
        return exportAsString(WhereFilter.empty());
    }

    private String exportAsString(WhereFilter filter) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        QifExport export = new QifExport(db, filter);
        export.export(bos);
        String s = new String(bos.toByteArray(), "UTF-8");
        Log.d("QIF", s);
        return s;
    }

}
