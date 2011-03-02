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
public class TransactionBuilder {

    private final DatabaseAdapter db;
    private final Transaction t = new Transaction();

    public static TransactionBuilder withDb(DatabaseAdapter db) {
        return new TransactionBuilder(db);
    }

    private TransactionBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public TransactionBuilder account(Account a) {
        t.fromAccountId = a.id;
        return this;
    }

    public TransactionBuilder amount(long amount) {
        t.fromAmount = amount;
        return this;
    }

    public TransactionBuilder payee(String payee) {
        t.payeeId = db.insertPayee(payee);
        return this;
    }

    public TransactionBuilder note(String note) {
        t.note = note;
        return this;
    }

    public TransactionBuilder category(Category c) {
        t.categoryId = c.id;
        return this;
    }

    public TransactionBuilder dateTime(DateTime dateTime) {
        t.dateTime = dateTime.asLong();
        return this;
    }

    public Transaction create() {
        long id = db.insertOrUpdate(t, null);
        t.id = id;
        return t;
    }

}
