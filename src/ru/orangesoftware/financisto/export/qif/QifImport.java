/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.qif;

import android.content.Context;
import android.util.Log;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.backup.FullDatabaseImport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ru.orangesoftware.financisto.export.qif.QifUtils.splitCategoryName;
import static ru.orangesoftware.financisto.utils.Utils.isEmpty;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 9/25/11 9:54 PM
 */
public class QifImport extends FullDatabaseImport {

    private final QifImportOptions options;

    private final Map<String, QifAccount> accountTitleToAccount = new HashMap<String, QifAccount>();
    private final Map<String, Long> payeeToId = new HashMap<String, Long>();
    private final Map<String, Category> categoryNameToCategory = new HashMap<String, Category>();
    private final CategoryTree<Category> categoryTree = new CategoryTree<Category>();

    public QifImport(Context context, DatabaseAdapter db, QifImportOptions options) {
        super(context, db);
        this.options = options;
    }

    @Override
    protected String[] tablesToClean() {
        List<String> backupTables = new ArrayList<String>(Arrays.asList(Backup.BACKUP_TABLES));
        backupTables.remove("currency");
        return backupTables.toArray(new String[backupTables.size()]);
    }

    @Override
    protected boolean shouldKeepSystemEntries() {
        return true;
    }

    @Override
    protected void restoreDatabase() throws IOException {
        doImport();
    }

    public void doImport() throws IOException {
        long t0 = System.currentTimeMillis();
        QifBufferedReader r = new QifBufferedReader(new BufferedReader(new InputStreamReader(new FileInputStream(options.filename), "UTF-8")));
        QifParser parser = new QifParser(r, options.dateFormat);
        parser.parse();
        long t1 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Parsing done in "+ TimeUnit.MILLISECONDS.toSeconds(t1-t0)+"s");
        doImport(parser);
        long t2 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Importing done in "+ TimeUnit.MILLISECONDS.toSeconds(t2-t1)+"s");
    }

    public void doImport(QifParser parser) {
        long t0 = System.currentTimeMillis();
        insertPayees(parser.payees);
        long t1 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Inserting payees done in "+ TimeUnit.MILLISECONDS.toSeconds(t1-t0)+"s");
        insertCategories(parser.categories);
        long t2 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Inserting categories done in "+ TimeUnit.MILLISECONDS.toSeconds(t2-t1)+"s");
        insertAccounts(parser.accounts);
        long t3 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Inserting accounts done in "+ TimeUnit.MILLISECONDS.toSeconds(t3-t2)+"s");
        insertTransactions(parser.accounts);
        long t4 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Inserting transactions done in "+ TimeUnit.MILLISECONDS.toSeconds(t4-t3)+"s");
    }

    private void insertPayees(Set<String> payees) {
        for (String payee : payees) {
            long id = dbAdapter.insertPayee(payee);
            payeeToId.put(payee, id);
        }
    }

    private void insertCategories(Set<QifCategory> categories) {
        for (QifCategory category : categories) {
            String name = splitCategoryName(category.name);
            insertCategory(name, category.isIncome);
        }
        categoryTree.sortByTitle();
        dbAdapter.insertCategoryTreeInTransaction(categoryTree);
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
            Account a = account.toAccount(options.currency);
            em.saveAccount(a);
            account.dbAccount = a;
            accountTitleToAccount.put(account.memo, account);
        }
    }

    private void insertTransactions(List<QifAccount> accounts) {
        long t0 = System.currentTimeMillis();
        reduceTransfers(accounts);
        long t1 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Reducing transfers done in "+ TimeUnit.MILLISECONDS.toSeconds(t1-t0)+"s");
        convertUnknownTransfers(accounts);
        long t2 = System.currentTimeMillis();
        Log.i("Financisto", "QIF Import: Converting transfers done in "+ TimeUnit.MILLISECONDS.toSeconds(t2-t1)+"s");
        int count = accounts.size();
        for (int i=0; i<count; i++) {
            long t3 = System.currentTimeMillis();
            QifAccount account = accounts.get(i);
            Account a = account.dbAccount;
            insertTransactions(a, account.transactions);
            // this might help GC
            account.transactions.clear();
            long t4 = System.currentTimeMillis();
            Log.i("Financisto", "QIF Import: Inserting transactions for account "+i+"/"+count+" done in "+ TimeUnit.MILLISECONDS.toSeconds(t4-t3)+"s");
        }
    }

    private void reduceTransfers(List<QifAccount> accounts) {
        for (QifAccount fromAccount : accounts) {
            List<QifTransaction> transactions = fromAccount.transactions;
            reduceTransfers(fromAccount, transactions);
        }
    }

    private void reduceTransfers(QifAccount fromAccount, List<QifTransaction> transactions) {
        for (QifTransaction fromTransaction : transactions) {
            if (fromTransaction.isTransfer() && fromTransaction.amount < 0) {
                boolean found = false;
                QifAccount toAccount = accountTitleToAccount.get(fromTransaction.toAccount);
                if (toAccount != null) {
                    Iterator<QifTransaction> iterator = toAccount.transactions.iterator();
                    while (iterator.hasNext()) {
                        QifTransaction toTransaction = iterator.next();
                        if (twoSidesOfTheSameTransfer(fromAccount, fromTransaction, toAccount, toTransaction)) {
                            iterator.remove();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    convertIntoRegularTransaction(fromTransaction);
                }
            }
            if (fromTransaction.splits != null) {
                reduceTransfers(fromAccount, fromTransaction.splits);
            }
        }
    }

    private void convertUnknownTransfers(List<QifAccount> accounts) {
        for (QifAccount fromAccount : accounts) {
            List<QifTransaction> transactions = fromAccount.transactions;
            convertUnknownTransfers(fromAccount, transactions);
        }
    }

    private void convertUnknownTransfers(QifAccount fromAccount/*to keep compiler happy*/, List<QifTransaction> transactions) {
        for (QifTransaction transaction : transactions) {
            if (transaction.isTransfer() && transaction.amount >= 0) {
                convertIntoRegularTransaction(transaction);
            }
            if (transaction.splits != null) {
                convertUnknownTransfers(fromAccount, transaction.splits);
            }
        }
    }

    private void convertIntoRegularTransaction(QifTransaction fromTransaction) {
        fromTransaction.memo = prependMemo("Transfer: " + fromTransaction.toAccount, fromTransaction);
        fromTransaction.toAccount = null;
    }

    private String prependMemo(String prefix, QifTransaction fromTransaction) {
        if (isEmpty(fromTransaction.memo)) {
            return prefix;
        } else {
            return prefix + " | " + fromTransaction.memo;
        }
    }

    private boolean twoSidesOfTheSameTransfer(QifAccount fromAccount, QifTransaction fromTransaction, QifAccount toAccount, QifTransaction toTransaction) {
        return toTransaction.isTransfer()
                && toTransaction.toAccount.equals(fromAccount.memo) && fromTransaction.toAccount.equals(toAccount.memo)
                && fromTransaction.date.equals(toTransaction.date) && fromTransaction.amount == -toTransaction.amount;
    }

    private void insertTransactions(Account a, List<QifTransaction> transactions) {
        for (QifTransaction transaction : transactions) {
            Transaction t = transaction.toTransaction();
            t.payeeId = findPayee(transaction.payee);
            t.fromAccountId = a.id;
            findToAccount(transaction, t);
            findCategory(transaction, t);
            if (transaction.splits != null) {
                List<Transaction> splits = new ArrayList<Transaction>(transaction.splits.size());
                for (QifTransaction split : transaction.splits) {
                    Transaction s = split.toTransaction();
                    findToAccount(split, s);
                    findCategory(split, s);
                    splits.add(s);
                }
                t.splits = splits;
            }
            dbAdapter.insertWithoutUpdatingBalance(t);
        }
    }

    public long findPayee(String payee) {
        if (payeeToId.containsKey(payee)) {
            return payeeToId.get(payee);
        }
        return 0;
    }

    private void findToAccount(QifTransaction transaction, Transaction t) {
        if (transaction.isTransfer()) {
            Account toAccount = findAccount(transaction.toAccount);
            if (toAccount != null) {
                t.toAccountId = toAccount.id;
                t.toAmount = -t.fromAmount;
            }
        }
    }

    private Account findAccount(String account) {
        QifAccount a = accountTitleToAccount.get(account);
        return a != null ? a.dbAccount : null;
    }

    private void findCategory(QifTransaction transaction, Transaction t) {
        Category c = findCategory(transaction.category);
        if (c != null) {
            t.categoryId = c.id;
        }
    }

    public Category findCategory(String category) {
        if (isEmpty(category)) {
            return null;
        }
        String name = splitCategoryName(category);
        return categoryNameToCategory.get(name);
    }

}