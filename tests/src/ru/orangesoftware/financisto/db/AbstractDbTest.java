package ru.orangesoftware.financisto.db;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/7/11 7:22 PM
 */
public class AbstractDbTest extends AndroidTestCase {

    protected DatabaseAdapter db;
    protected MyEntityManager em;

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

}
