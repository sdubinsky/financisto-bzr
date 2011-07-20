package ru.orangesoftware.financisto.model;

import android.content.Intent;
import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/29/11 1:00 AM
 */
public class TransactionTest extends AbstractDbTest {

    public void test_should_create_splits() {
        Account a1 = AccountBuilder.createDefault(db);
        Account a2 = AccountBuilder.createDefault(db);
        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(200).payee("P1").category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), 60)
                .withSplit(categories.get("A2"), 40)
                .withTransferSplit(a2, 100, 50)
                .create();
        List<Transaction> splits = db.em().getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
        Transaction split1 = splits.get(0);
        assertEquals(t.payeeId, split1.payeeId);
        assertEquals(a1.id, split1.fromAccountId);
        assertEquals(60, split1.fromAmount);
        assertEquals(categories.get("A1").id, split1.categoryId);
        Transaction split2 = splits.get(1);
        assertEquals(t.payeeId, split2.payeeId);
        assertEquals(a1.id, split2.fromAccountId);
        assertEquals(40, split2.fromAmount);
        assertEquals(categories.get("A2").id, split2.categoryId);
        Transaction split3 = splits.get(2);
        assertEquals(t.payeeId, split3.payeeId);
        assertEquals(a1.id, split3.fromAccountId);
        assertEquals(a2.id, split3.toAccountId);
        assertEquals(100, split3.fromAmount);
        assertEquals(50, split3.toAmount);
    }

    public void test_should_duplicate_splits() {
        Account a1 = AccountBuilder.createDefault(db);
        Account a2 = AccountBuilder.createDefault(db);
        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-150).category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), -60)
                .withSplit(categories.get("A2"), -40)
                .withTransferSplit(a2, -50, 40)
                .create();
        List<Transaction> splits = db.em().getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
        long newId = db.duplicateTransaction(t.id);
        assertNotSame(t.id, newId);
        List<Transaction> newSplits = db.em().getSplitsForTransaction(newId);
        assertEquals(3, newSplits.size());
        assertEquals(-150, newSplits.get(0).fromAmount+newSplits.get(1).fromAmount+newSplits.get(2).fromAmount);
    }

    public void test_should_delete_splits() {
        Account a1 = AccountBuilder.createDefault(db);
        Account a2 = AccountBuilder.createDefault(db);
        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);
        Transaction t = TransactionBuilder.withDb(db).account(a1).amount(-150).category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), -60)
                .withSplit(categories.get("A2"), -40)
                .withTransferSplit(a2, -50, 40)
                .create();
        List<Transaction> splits = db.em().getSplitsForTransaction(t.id);
        assertEquals(3, splits.size());
        db.deleteTransaction(t.id);
        splits = db.em().getSplitsForTransaction(t.id);
        assertEquals(0, splits.size());
    }

    public void test_should_store_transaction_in_the_database() {
        Transaction t = new Transaction();
        t.fromAccountId = 1;
        t.fromAmount = 1000;
        t.categoryId = 5;
        t.accuracy = 6.0f;
        t.latitude = -11.0;
        t.isCCardPayment = 1;
        t.note = "My note";
        t.status = TransactionStatus.RS;
        long id = em.saveOrUpdate(t);
        assertTrue(id > 0);
        Transaction restored = em.load(Transaction.class, id);
        assertEquals(t.fromAccountId, restored.fromAccountId);
        assertEquals(t.fromAmount, restored.fromAmount);
        assertEquals(t.categoryId, restored.categoryId);
        assertEquals(t.note, restored.note);
        assertEquals(t.status, restored.status);
        assertEquals(t.accuracy, restored.accuracy);
        assertEquals(t.latitude, restored.latitude);
        assertEquals(t.isCCardPayment, restored.isCCardPayment);
    }

    public void test_should_restore_split_from_intent() {
        Transaction split = new Transaction();
        split.id = -2;
        split.fromAccountId = 3;
        split.toAccountId = 5;
        split.categoryId = 7;
        split.fromAmount = -10000;
        split.toAmount = 4000;
        split.unsplitAmount = 300000;
        split.note = "My note";
        Intent intent = new Intent();
        split.toIntentAsSplit(intent);
        Transaction restored = Transaction.fromIntentAsSplit(intent);
        assertEquals(split.id, restored.id);
        assertEquals(split.fromAccountId, restored.fromAccountId);
        assertEquals(split.toAccountId, restored.toAccountId);
        assertEquals(split.categoryId, restored.categoryId);
        assertEquals(split.fromAmount, restored.fromAmount);
        assertEquals(split.toAmount, restored.toAmount);
        assertEquals(split.unsplitAmount, restored.unsplitAmount);
        assertEquals(split.note, restored.note);
    }

}
