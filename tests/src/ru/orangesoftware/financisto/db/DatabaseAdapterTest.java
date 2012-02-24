package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.test.*;
import ru.orangesoftware.orb.EntityManager;

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

    public void test_should_remember_last_used_transaction_for_the_payee() {
        //when
        TransactionBuilder.withDb(db).account(a1).amount(1000).payee("Payee1").category(categoriesMap.get("A1")).create();
        //then
        Payee p = em.getPayee("Payee1");
        assertEquals(categoriesMap.get("A1").id, p.lastCategoryId);
    }

    public void test_should_search_payee_with_or_without_first_letter_capitalized() {
        // given
        em.insertPayee("Парковка");
        em.insertPayee("parking");

        //then
        assertEquals("parking", fetchFirstPayee("P"));
        assertEquals("parking", fetchFirstPayee("p"));
        assertEquals("parking", fetchFirstPayee("Pa"));
        assertEquals("parking", fetchFirstPayee("par"));
        assertEquals("Парковка", fetchFirstPayee("П"));
        assertEquals("Парковка", fetchFirstPayee("п"));
        assertEquals("Парковка", fetchFirstPayee("Па"));
        assertEquals("Парковка", fetchFirstPayee("пар"));
    }

    public void test_should_detect_multiple_account_currencies() {
        // one account only
        assertTrue(db.singleCurrencyOnly());

        // two accounts with the same currency
        AccountBuilder.withDb(db).currency(a1.currency).title("Account2").create();
        assertTrue(db.singleCurrencyOnly());

        //another account with a different currency, but not included into totals
        Currency c2 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        AccountBuilder.withDb(db).currency(c2).title("Account3").doNotIncludeIntoTotals().create();
        assertTrue(db.singleCurrencyOnly());

        //this account is not active
        AccountBuilder.withDb(db).currency(c2).title("Account4").inactive().create();
        assertTrue(db.singleCurrencyOnly());

        //now it's two currencies
        AccountBuilder.withDb(db).currency(c2).title("Account5").create();
        assertFalse(db.singleCurrencyOnly());
    }

    public void test_should_not_return_split_category_as_parent_when_editing_a_category() {
        List<Category> list = db.getCategoriesWithoutSubtreeAsList(categoriesMap.get("A").id);
        for (Category category : list) {
            assertFalse("Should not be split", category.isSplit());
        }
    }

    public void test_should_return_only_valid_parent_categories_when_editing_a_category() {
        List<Category> list = db.getCategoriesWithoutSubtreeAsList(categoriesMap.get("A").id);
        assertEquals(2, list.size());
        assertEquals(Category.NO_CATEGORY_ID, list.get(0).id);
        assertEquals(categoriesMap.get("B").id, list.get(1).id);
    }
    
    private String fetchFirstPayee(String s) {
        Cursor c = em.getAllPayeesLike(s);
        try {
            if (c.moveToFirst()) {
                Payee p =  EntityManager.loadFromCursor(c, Payee.class);
                return p.title;
            }
            return null;
        } finally {
            c.close();
        }
    }

}
