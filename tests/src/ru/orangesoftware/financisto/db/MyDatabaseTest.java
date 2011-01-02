package ru.orangesoftware.financisto.db;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;

public class MyDatabaseTest extends AndroidTestCase {

    DatabaseAdapter db;
    MyEntityManager em;

    @Override
    public void setUp() throws Exception {
        Context context = new RenamingDelegatingContext(getContext(), "test-");
        db = new DatabaseAdapter(context);
        db.open();
        em = db.em();
    }

    @Override
    public void tearDown() throws Exception {
        db.close();
    }

    public void testShouldSavePayeeWhenTransactionInserted() {
        // given
        String payee = "Payee1";
        Transaction t = new Transaction();
        t.payee = payee;
        // when
        long transactionId = db.insertOrUpdate(t, null);
        // then
        assertTrue("Transaction should be inserted!", transactionId > 0);
        assertTrue("Payee should be inserted!", t.payeeId > 0);
    }

    public void testShouldSavePayeeOnlyOnce() {
        // given
        String payee = "Payee1";
        // when
        Payee p1 = em.insertPayee(payee);
        Payee p2 = em.insertPayee(payee);
        ArrayList<Payee> payees = em.getAllPayeeList();
        // then
        assertEquals("Ids should be the same!", p1.id, p2.id);
        assertEquals("List should be of size 1!", 1, payees.size());
        assertEquals("The first payee should be the one!", payees.get(0).title, payee);
    }

    public void testShouldCheckThatWhenChildCategoryIsInsertedThenItInheritsTypeFromTheParent() {
        long a1Id = db.insertOrUpdate(createIncomeCategory("A1"), new ArrayList<Attribute>());
        long a2Id = db.insertOrUpdate(createExpenseCategory("A2"), new ArrayList<Attribute>());
        long a11id = db.insertChildCategory(a1Id, createExpenseCategory("a11"));
        long a21id = db.insertChildCategory(a2Id, createIncomeCategory("a21"));
        Category a1 = db.getCategory(a1Id);
        Category a2 = db.getCategory(a2Id);
        Category a11 = db.getCategory(a11id);
        Category a21 = db.getCategory(a21id);
        assertTrue(a1.isIncome());
        assertTrue(a2.isExpense());
        assertTrue(a11.isIncome());
        assertTrue(a21.isExpense());
    }

    public void testShouldCheckThatWhenCategoryMovesUnderANewParentThenItInheritsTypeFromTheNewParent() {
        long a1Id = db.insertOrUpdate(createIncomeCategory("A1"), new ArrayList<Attribute>());
        long a2Id = db.insertOrUpdate(createExpenseCategory("A2"), new ArrayList<Attribute>());
        long a11Id = db.insertChildCategory(a1Id, createExpenseCategory("a11"));
        long a111Id = db.insertChildCategory(a11Id, createExpenseCategory("a111"));
        Category a2 = db.getCategory(a2Id);
        Category a11 = db.getCategory(a11Id);
        assertTrue(a11.isIncome());
        a11.parent = a2;
        a11.title = "a21";
        long a21id = db.insertOrUpdate(a11, new ArrayList<Attribute>());
        Category a21 = db.getCategory(a21id);
        Category a211 = db.getCategory(a111Id);
        assertTrue("Category should inherit new type", a21.isExpense());
        assertTrue("Child category should inherit new type", a211.isExpense());
    }

    private Category createIncomeCategory(String title) {
        Category c = new Category();
        c.title = title;
        c.makeThisCategoryIncome();
        return c;
    }

    private Category createExpenseCategory(String title) {
        Category c = new Category();
        c.title = title;
        c.makeThisCategoryExpense();
        return c;
    }

}
