package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.*;

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

    public TransactionBuilder location(String location) {
        MyLocation myLocation = new MyLocation();
        myLocation.name = location;
        t.locationId = db.em().saveLocation(myLocation);
        return this;
    }

    public TransactionBuilder project(String project) {
        Project myProject = new Project();
        myProject.title = project;
        t.projectId = db.em().saveOrUpdate(myProject);
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

    public TransactionBuilder ccPayment() {
        t.isCCardPayment = 1;
        return this;
    }

    public TransactionBuilder dateTime(DateTime dateTime) {
        t.dateTime = dateTime.asLong();
        return this;
    }

    public TransactionBuilder scheduleOnce(DateTime dateTime) {
        t.dateTime = dateTime.asLong();
        t.setAsScheduled();
        return this;
    }

    public TransactionBuilder scheduleRecur(String pattern) {
        t.recurrence = pattern;
        t.setAsScheduled();
        return this;
    }

    public TransactionBuilder withSplit(Category category, long amount) {
        return withSplit(category, amount, null);
    }

    public TransactionBuilder withSplit(Category category, long amount, String note) {
        Transaction split = new Transaction();
        split.categoryId = category.id;
        split.fromAmount = amount;
        split.note = note;
        t.splits.add(split);
        t.categoryId = Category.SPLIT_CATEGORY_ID;
        return this;
    }

    public TransactionBuilder withTransferSplit(Account toAccount, long fromAmount, long toAmount) {
        Transaction split = new Transaction();
        split.toAccountId = toAccount.id;
        split.fromAmount = fromAmount;
        split.toAmount = toAmount;
        t.splits.add(split);
        t.categoryId = Category.SPLIT_CATEGORY_ID;
        return this;
    }

    public Transaction create() {
        t.id = db.insertOrUpdate(t);
        return t;
    }

}
