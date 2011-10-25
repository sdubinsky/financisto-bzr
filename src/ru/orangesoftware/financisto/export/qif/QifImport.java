/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.qif;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.*;

import java.util.*;

import static ru.orangesoftware.financisto.export.qif.QifUtils.splitCategoryName;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 9/25/11 9:54 PM
 */
public class QifImport {

    private final DatabaseAdapter db;
    private final MyEntityManager em;

    private final Map<String, Long> payeeToId = new HashMap<String, Long>();
    private final Map<String, Category> categoryNameToCategory = new HashMap<String, Category>();
    private final CategoryTree<Category> categoryTree = new CategoryTree<Category>();

    public QifImport(DatabaseAdapter db) {
        this.db = db;
        this.em = db.em();
    }

    public void doImport(QifParser parser) {
        insertPayees(parser.payees);
        insertCategories(parser.categories);
        insertAccounts(parser.accounts);
    }

    private void insertPayees(Set<String> payees) {
        db.db().beginTransaction();
        try {
            for (String payee : payees) {
                long id = db.insertPayee(payee);
                payeeToId.put(payee, id);
            }
            db.db().setTransactionSuccessful();
        } finally {
            db.db().endTransaction();
        }
    }

    private void insertCategories(List<QifCategory> categories) {
        for (QifCategory category : categories) {
            String name = splitCategoryName(category.name);
            insertCategory(name, category.isIncome);
        }
        categoryTree.reIndex();
        db.insertCategoryTree(categoryTree);
    }

    private Category insertCategory(String name, boolean isIncome) {
        if (isChildCategory(name)) {
            return insertChildCategory(name, isIncome);
        } else {
            return insertRootCategory(name, isIncome);
        }
    }

    private boolean isChildCategory(String name) {
        return name.contains(":");
    }

    private Category insertRootCategory(String name, boolean income) {
        Category c = categoryNameToCategory.get(name);
        if (c == null) {
            c = createCategoryInCache(name, name, income);
            categoryTree.add(c);
        }
        return c;
    }

    private Category createCategoryInCache(String fullName, String name, boolean income) {
        Category c;
        c = new Category();
        c.title = name;
        if (income) {
            c.makeThisCategoryIncome();
        }
        categoryNameToCategory.put(fullName, c);
        return c;
    }

    private Category insertChildCategory(String name, boolean income) {
        int i = name.lastIndexOf(':');
        String parentCategoryName = name.substring(0, i);
        String childCategoryName = name.substring(i+1);
        Category parent = insertCategory(parentCategoryName, income);
        Category child = createCategoryInCache(name, childCategoryName, income);
        parent.addChild(child);
        return child;
    }

    private void insertAccounts(List<QifAccount> accounts) {
        for (QifAccount account : accounts) {
            db.db().beginTransaction();
            try {
                Account a = account.toAccount();
                em.saveAccount(a);
                insertTransactions(a, account.transactions);
                db.db().setTransactionSuccessful();
            } finally {
                db.db().endTransaction();
            }
        }
    }

    private void insertTransactions(Account a, List<QifTransaction> transactions) {
        for (QifTransaction transaction : transactions) {
            Transaction t = transaction.toTransaction();
            t.payeeId = findPayee(transaction.payee);
            t.fromAccountId = a.id;
            Category c = findCategory(transaction.category);
            if (c != null) {
                t.categoryId = c.id;
            }
            db.insertOrUpdateInTransaction(t, Collections.<TransactionAttribute>emptyList());
        }
    }

    public Category findCategory(String category) {
        String name = splitCategoryName(category);
        return categoryNameToCategory.get(name);
    }

    public long findPayee(String payee) {
        if (payeeToId.containsKey(payee)) {
            return payeeToId.get(payee);
        }
        return 0;
    }

}
