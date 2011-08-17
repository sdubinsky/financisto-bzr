package ru.orangesoftware.financisto.db;

import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.RestoredTransaction;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.CategoryBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseAdapterTest extends AbstractDbTest {

    Account a1;
    Map<String, Category> categoriesMap;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        a1 = AccountBuilder.createDefault(db);
        categoriesMap = CategoryBuilder.createDefaultHierarchy(db);
    }

    public void test_should_restore_split_transaction() {
        //given
        Transaction originalTransaction = TransactionBuilder.withDb(db).account(a1).amount(100)
                .withSplit(categoriesMap.get("A1"), 40)
                .withSplit(categoriesMap.get("B"), 60)
                .create();
        List<RestoredTransaction> transactionsToRestore = new ArrayList<RestoredTransaction>();
        transactionsToRestore.add(new RestoredTransaction(originalTransaction.id, DateTime.date(2011, 8, 16).atNoon().asDate()));
        //when
        long[] restoredIds = db.storeMissedSchedules(transactionsToRestore, DateTime.date(2011, 8, 16).atMidnight().asLong());
        //then
        assertNotNull(restoredIds);
        assertEquals(1, restoredIds.length);
        Transaction restoredTransaction = db.getTransaction(restoredIds[0]);
        assertNotNull(restoredTransaction);
        assertTrue(restoredTransaction.isSplitParent());
        List<Transaction> splits = em.getSplitsForTransaction(restoredIds[0]);
        assertNotNull(splits);
        assertEquals(2, splits.size());
    }

}
