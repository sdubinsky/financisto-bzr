package ru.orangesoftware.financisto.export;

import android.util.Log;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.qif.QifExport;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.test.CurrencyBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import static ru.orangesoftware.financisto.test.DateTime.date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/4/11 10:23 PM
 */
public class QIFExportTest extends AbstractDbTest {

    Account a1;
    Account a2;

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

    public void test_should_export_transaction_amount_according_to_the_config() throws Exception {
        Account a = createFirstAccount();
        Currency c = new Currency();
        c.decimals = 1;
        c.decimalSeparator = "','";
        c.groupSeparator = "''";
        c.symbol = "";
        TransactionBuilder.withDb(db).account(a).amount(-210056).payee("Payee 1").dateTime(date(2011, 2, 7)).create();
        assertEquals(
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n"+
                "!Type:Cash\n"+
                "D07/02/2011\n"+
                "T-2100,6\n"+
                "PPayee 1\n"+
                "^\n",
                exportAsString(c));
    }

    public void test_should_export_transaction_date_according_to_the_config() throws Exception {
        Account a = createFirstAccount();
        TransactionBuilder.withDb(db).account(a).amount(-210056).payee("Payee 1").dateTime(date(2011, 7, 10)).create();
        assertEquals(
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n"+
                "!Type:Cash\n"+
                "D10/07/2011\n"+
                "T-2,100.56\n"+
                "PPayee 1\n"+
                "^\n",
                exportAsString("dd/MM/yyyy"));
        assertEquals(
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n"+
                "!Type:Cash\n"+
                "D07/10/2011\n"+
                "T-2,100.56\n"+
                "PPayee 1\n"+
                "^\n",
                exportAsString("MM/dd/yyyy"));
        assertEquals(
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n"+
                "!Type:Cash\n"+
                "D2011-07-10\n"+
                "T-2,100.56\n"+
                "PPayee 1\n"+
                "^\n",
                exportAsString("yyyy-MM-dd"));
    }

    public void test_should_export_account_with_a_couple_of_transactions() throws Exception {
        Account a = createFirstAccount();
        Category p1 = createExpenseCategory("P1");
        Category c1 = createCategory(p1, "c1");
        TransactionBuilder.withDb(db).account(a).amount(1000).category(p1).dateTime(date(2011, 2, 8)).create();
        TransactionBuilder.withDb(db).account(a).amount(-2056).category(c1).payee("Payee 1").note("Some note here...").dateTime(date(2011, 2, 7)).create();
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

    public void test_should_export_only_selected_accounts() throws Exception {
        createSampleData();
        assertEquals(
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
                exportAsString(new long[]{a2.id}));
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
        TransferBuilder.withDb(db).fromAccount(a2).fromAmount(-2000).toAccount(a1).toAmount(2000).dateTime(date(2011, 2, 8)).create();
    }

    public void test_should_export_categories() throws Exception {
        createCategories();
        assertEquals(
                "!Type:Cat\n" +
                        "NA1\n" +
                        "I\n" +
                        "^\n" +
                        "NA1:aa1\n" +
                        "I\n" +
                        "^\n" +
                        "NA1:aa2\n" +
                        "I\n" +
                        "^\n" +
                        "NB2\n" +
                        "E\n" +
                        "^\n" +
                        "NB2:bb1\n" +
                        "E\n" +
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
        String start = String.valueOf(date(2011, 2, 1).atMidnight().asLong());
        String end = String.valueOf(date(2011, 2, 28).atDayEnd().asLong());
        return WhereFilter.empty().btw(BlotterFilter.DATETIME, start, end);
    }

    private void createSampleData() {
        a1 = createFirstAccount();
        TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(date(2011, 2, 8)).create();
        TransactionBuilder.withDb(db).account(a1).amount(-2345).dateTime(date(2011, 2, 7)).create();
        TransactionBuilder.withDb(db).account(a1).amount(-6780).dateTime(date(2011, 1, 1).atNoon()).create();
        a2 = createSecondAccount();
        TransactionBuilder.withDb(db).account(a2).amount(-2000).dateTime(date(2011, 2, 8)).create();
        TransactionBuilder.withDb(db).account(a2).amount(5400).dateTime(date(2011, 1, 2).atMidnight()).create();
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
        Currency c = CurrencyBuilder.withDb(db)
                .title("Singapore Dollar")
                .name("SGD")
                .symbol("S$")
                .create();
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
        QifExportOptions options = new QifExportOptions(Currency.EMPTY, QifExportOptions.DEFAULT_DATE_FORMAT, null, WhereFilter.empty());
        return exportAsString(options);
    }

    private String exportAsString(Currency currency) throws Exception {
        QifExportOptions options = new QifExportOptions(currency, QifExportOptions.DEFAULT_DATE_FORMAT, null, WhereFilter.empty());
        return exportAsString(options);
    }

    private String exportAsString(String dateFormat) throws Exception {
        QifExportOptions options = new QifExportOptions(Currency.EMPTY, dateFormat, null, WhereFilter.empty());
        return exportAsString(options);
    }

    private String exportAsString(WhereFilter filter) throws Exception {
        QifExportOptions options = new QifExportOptions(Currency.EMPTY, QifExportOptions.DEFAULT_DATE_FORMAT, null, filter);
        return exportAsString(options);
    }

    private String exportAsString(long[] accounts) throws Exception {
        QifExportOptions options = new QifExportOptions(Currency.EMPTY, QifExportOptions.DEFAULT_DATE_FORMAT, accounts, WhereFilter.empty());
        return exportAsString(options);
    }

    private String exportAsString(QifExportOptions options) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        QifExport export = new QifExport(db, options);
        export.export(bos);
        String s = new String(bos.toByteArray(), "UTF-8");
        Log.d("QIF", s);
        return s;
    }

}
