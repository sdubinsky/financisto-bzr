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
public class MockTransaction {

    private final DatabaseAdapter db;
    private final Transaction t = new Transaction();

    private MockTransaction(DatabaseAdapter db) {
        this.db = db;
    }

    public static MockTransaction withDb(DatabaseAdapter db) {
        return new MockTransaction(db);
    }

    public MockTransaction account(Account a) {
        t.fromAccountId = a.id;
        return this;
    }

    public MockTransaction amount(long amount) {
        t.fromAmount = amount;
        return this;
    }

    public MockTransaction payee(String payee) {
        t.payeeId = db.insertPayee(payee);
        return this;
    }

    public MockTransaction note(String note) {
        t.note = note;
        return this;
    }

    public MockTransaction category(Category c) {
        t.categoryId = c.id;
        return this;
    }

    public MockTransaction dateTime(MockDateTime dateTime) {
        t.dateTime = dateTime.asLong();
        return this;
    }

    public Transaction create() {
        long id = db.insertOrUpdate(t, null);
        t.id = id;
        return t;
    }

}
