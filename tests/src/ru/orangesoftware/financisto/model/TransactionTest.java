package ru.orangesoftware.financisto.model;

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
        Account a = AccountBuilder.createDefault(db);
        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(100).category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), 60)
                .withSplit(categories.get("A2"), 40)
                .create();
        List<Split> splits = db.em().getSplitsForTransaction(t.id);
        assertNotNull(splits);
        assertEquals(2, splits.size());
        assertEquals(100, splits.get(0).amount+splits.get(1).amount);
    }

    public void test_should_duplicate_splits() {
        Account a = AccountBuilder.createDefault(db);
        Map<String, Category> categories = CategoryBuilder.createDefaultHierarchy(db);
        Transaction t = TransactionBuilder.withDb(db).account(a).amount(100).category(CategoryBuilder.split(db))
                .withSplit(categories.get("A1"), 60)
                .withSplit(categories.get("A2"), 40)
                .create();
        List<Split> splits = db.em().getSplitsForTransaction(t.id);
        assertNotNull(splits);
        assertEquals(2, splits.size());
        long newId = db.duplicateTransaction(t.id);
        assertNotSame(t.id, newId);
        List<Split> newSplits = db.em().getSplitsForTransaction(newId);
        assertEquals(2, newSplits.size());
        assertEquals(100, newSplits.get(0).amount+newSplits.get(1).amount);
    }

}
