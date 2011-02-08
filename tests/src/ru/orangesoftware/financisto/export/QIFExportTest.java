package ru.orangesoftware.financisto.export;

import android.util.Log;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.qif.QifExport;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.MockDate;

import java.io.ByteArrayOutputStream;

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
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n",
                exportAsString());
    }

    public void test_should_export_account_with_a_couple_of_transactions() throws Exception {
        Account a = createFirstAccount();
        createTransaction(a, 1000, MockDate.thisYear().feb().day(8).asLong());
        createTransaction(a, -2056, MockDate.thisYear().feb().day(7).asLong());
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
                "T-20.56\n"+
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

    private WhereFilter createFebruaryOnlyFilter() {
        String start = String.valueOf(MockDate.thisYear().feb().day(1).atMidnight().asLong());
        String end = String.valueOf(MockDate.thisYear().feb().day(28).atDayEnd().asLong());
        return WhereFilter.empty().btw(BlotterFilter.DATETIME, start, end);
    }

    private void createSampleData() {
        Account a1 = createFirstAccount();
        createTransaction(a1, 1000, MockDate.thisYear().feb().day(8).asLong());
        createTransaction(a1, -2345, MockDate.thisYear().feb().day(7).asLong());
        createTransaction(a1, -6780, MockDate.thisYear().jan().day(1).atNoon().asLong());
        Account a2 = createSecondAccount();
        createTransaction(a2, -2000, MockDate.thisYear().feb().day(8).asLong());
        createTransaction(a2, 5400, MockDate.thisYear().jan().day(2).atMidnight().asLong());
    }

    private Transaction createTransaction(Account a, long amount, long datetime) {
        Transaction t1 = new Transaction();
        t1.fromAccountId = a.id;
        t1.fromAmount = amount;
        t1.dateTime = datetime;
        db.insertOrUpdate(t1, null);
        return t1;
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
