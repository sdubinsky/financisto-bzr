package ru.orangesoftware.financisto.db;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Transaction;

import java.util.ArrayList;

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
        assertEquals("Ids are not the same!", p1.id, p2.id);
        assertEquals("List should be of size 1!", 1, payees.size());
        assertEquals("The first payee should be the one!", payees.get(0).title, payee);
    }

}
