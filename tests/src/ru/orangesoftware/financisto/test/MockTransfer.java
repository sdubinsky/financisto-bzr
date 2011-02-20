package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/13/11 8:52 PM
 */
public class MockTransfer {

    private final DatabaseAdapter db;
    private final Transaction t = new Transaction();

    private MockTransfer(DatabaseAdapter db) {
        this.db = db;
    }

    public static MockTransfer withDb(DatabaseAdapter db) {
        return new MockTransfer(db);
    }

    public MockTransfer fromAccount(Account a) {
        t.fromAccountId = a.id;
        return this;
    }

    public MockTransfer fromAmount(long amount) {
        t.fromAmount = amount;
        return this;
    }

    public MockTransfer toAccount(Account a) {
        t.toAccountId = a.id;
        return this;
    }

    public MockTransfer toAmount(long amount) {
        t.toAmount = amount;
        return this;
    }

    public MockTransfer dateTime(MockDateTime dateTime) {
        t.dateTime = dateTime.asLong();
        return this;
    }

    public Transaction create() {
        long id = db.insertOrUpdate(t, null);
        t.id = id;
        return t;
    }

}
