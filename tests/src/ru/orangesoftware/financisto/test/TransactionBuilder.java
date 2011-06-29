package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;

import java.util.LinkedList;

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
        this.t.splits = new LinkedList<Transaction>();
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

    public TransactionBuilder withSplit(Category category, long amount) {
        Transaction split = new Transaction();
        split.categoryId = category.id;
        split.fromAmount = amount;
        t.splits.add(split);
        return this;
    }

    public TransactionBuilder withTransferSplit(Account toAccount, long fromAmount, long toAmount) {
        Transaction split = new Transaction();
        split.toAccountId = toAccount.id;
        split.fromAmount = fromAmount;
        split.toAmount = toAmount;
        t.splits.add(split);
        return this;
    }

    public Transaction create() {
        t.id = db.insertOrUpdate(t);
        return t;
    }

}
