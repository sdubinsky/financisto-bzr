package ru.orangesoftware.financisto.db;

import android.content.Context;
import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/7/11 7:22 PM
 */
public abstract class AbstractDbTest extends AndroidTestCase {

    private DatabaseHelper dbHelper;
    protected DatabaseAdapter db;
    protected MyEntityManager em;

    @Override
    public void setUp() throws Exception {
        Context context = new RenamingDelegatingContext(getContext(), "test-");
        dbHelper = new DatabaseHelper(context);
        db = new DatabaseAdapter(context, dbHelper);
        db.open();
        em = db.em();
    }

    @Override
    public void tearDown() throws Exception {
        dbHelper.close();
    }

    public void assertAccountTotal(Account account, long total) {
        Account a = db.em().getAccount(account.id);
        assertEquals(total, a.totalAmount);
    }

    public void assertFinalBalanceForAccount(Account a, long expectedBalance) {
        Cursor c = db.db().rawQuery("select balance from running_balance where account_id=? order by datetime desc, transaction_id desc limit 1",
                new String[]{String.valueOf(a.id)});
        try {
            if (c.moveToFirst()) {
                long balance = c.getLong(0);
                assertEquals(expectedBalance, balance);
            } else {
                fail();
            }
        } finally {
            c.close();
        }
    }

    public void assertAccountBalanceForTransaction(Transaction t, Account a, long expectedBalance) {
        Cursor c = db.db().rawQuery("select balance from running_balance where account_id=? and transaction_id=?",
                new String[]{String.valueOf(a.id), String.valueOf(t.id)});
        try {
            if (c.moveToFirst()) {
                long balance = c.getLong(0);
                assertEquals(expectedBalance, balance);
            } else {
                fail();
            }
        } finally {
            c.close();
        }
    }

}
