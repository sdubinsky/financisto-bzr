package ru.orangesoftware.financisto.db;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;

public class DatabaseAdapterTest extends AbstractDbTest {

    public void test_should_update_account_total_when_credit_transaction_is_added() {
        Account a = AccountBuilder.createDefault(db);

        TransactionBuilder.withDb(db).account(a).amount(1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(1000L, a.totalAmount);

        TransactionBuilder.withDb(db).account(a).amount(1234).create();
        a = db.em().getAccount(a.id);
        assertEquals(2234L, a.totalAmount);
    }

    public void test_should_update_account_total_when_debit_transaction_is_added() {
        Account a = AccountBuilder.createDefault(db);

        TransactionBuilder.withDb(db).account(a).amount(-1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(-1000L, a.totalAmount);

        TransactionBuilder.withDb(db).account(a).amount(-1234).create();
        a = db.em().getAccount(a.id);
        assertEquals(-2234L, a.totalAmount);
    }

    public void test_should_update_account_total_when_credit_transaction_is_updated_with_bigger_amount() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(1000L, a.totalAmount);

        t.fromAmount = 1234;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(1234L, a.totalAmount);
    }

    public void test_should_update_account_total_when_credit_transaction_is_updated_with_lesser_amount() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(1000L, a.totalAmount);

        t.fromAmount = 900;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(900L, a.totalAmount);
    }

    public void test_should_update_account_total_when_debit_transaction_is_updated_with_lesser_amount() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(-1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(-1000L, a.totalAmount);

        t.fromAmount = -900;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(-900L, a.totalAmount);
    }

    public void test_should_update_account_total_when_debit_transaction_is_updated_with_bigger_amount() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(-1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(-1000L, a.totalAmount);

        t.fromAmount = -1920;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(-1920L, a.totalAmount);
    }

    public void test_should_update_account_total_when_credit_transaction_is_converted_to_debit_and_back() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(1000L, a.totalAmount);

        t.fromAmount = -550;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(-550, a.totalAmount);

        t.fromAmount = 226;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(226, a.totalAmount);
    }

    public void test_should_update_account_total_when_debit_transaction_is_converted_to_credit_and_back() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(-1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(-1000L, a.totalAmount);

        t.fromAmount = 245;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(245, a.totalAmount);

        t.fromAmount = -110;
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(-110, a.totalAmount);
    }

    public void test_should_update_account_total_after_mix_of_debit_and_credit_transactions() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(1000).create();
        a = db.em().getAccount(a.id);
        assertEquals(1000L, a.totalAmount);

        t.fromAmount = 900; // 1000 - 100
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(900, a.totalAmount);

        t = TransactionBuilder.withDb(db).account(a).amount(-123).create();  // 900 - 123
        a = db.em().getAccount(a.id);
        assertEquals(777L, a.totalAmount);

        t.fromAmount = -120; // 777 + 3
        db.insertOrUpdate(t);
        a = db.em().getAccount(a.id);
        assertEquals(780, a.totalAmount);

        TransactionBuilder.withDb(db).account(a).amount(100).create();  // 780 + 100
        a = db.em().getAccount(a.id);
        assertEquals(880L, a.totalAmount);

        TransactionBuilder.withDb(db).account(a).amount(-80).create();  // 880 - 80
        a = db.em().getAccount(a.id);
        assertEquals(800L, a.totalAmount);

    }

    //TODO: add tests for transfers

}
