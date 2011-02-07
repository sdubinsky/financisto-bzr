package ru.orangesoftware.financisto.export;

import android.util.Log;
import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.qif.QifExport;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/4/11 10:23 PM
 */
public class QIFExportTest extends AbstractDbTest {

    QifExport export;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        export = new QifExport(db);
    }

    public void test_should_export_empty_file() throws Exception {
        String output = exportAsString();
        assertNotNull(output);
        assertTrue(output.length() == 0);
    }

    public void test_should_export_empty_account() throws Exception {
        createEmptyAccount();
        assertEquals(
                "!Account\n"+
                "NMy Cash Account\n"+
                "TCash\n"+
                "^\n",
                exportAsString());
    }

    public void test_should_export_account_with_a_couple_of_transactions() throws Exception {
        Account a = createEmptyAccount();
        Transaction t1 = new Transaction();
        t1.fromAccountId = a.id;
        t1.fromAmount = 1000;
        db.insertOrUpdate(t1, null);
        Transaction t2 = new Transaction();
        t2.fromAccountId = a.id;
        t2.fromAmount = -2056;
        db.insertOrUpdate(t1, null);
        assertEquals(
                "!Account\n" +
                "NMy Cash Account\n" +
                "TCash\n" +
                "^\n" +
                "!Type:Cash\n" +
                "D" + formatToday() + "\n" +
                "T10.00\n" +
                "^\n",
                "D" + formatToday() + "\n" +
                "T-20.56\n" +
                "^\n",
                exportAsString());
    }

    private String formatToday() {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        return df.format(new Date());
    }

    private Account createEmptyAccount() {
        Currency c = new Currency();
        c.title = "Singapore Dollar";
        c.name = "SGD";
        c.symbol = "S$";
        em.saveOrUpdate(c);
        assertNotNull(em.load(Currency.class, c.id));
        Account a = new Account();
        a.title = "My Cash Account";
        a.type = AccountType.CASH.name();
        a.currency = c;
        a.totalAmount = 1000;
        em.saveAccount(a);
        assertNotNull(em.load(Account.class, a.id));
        return a;
    }

    private String exportAsString() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        export.export(bos);
        String s = new String(bos.toByteArray(), "UTF-8");
        Log.d("QIF", s);
        return s;
    }

}
