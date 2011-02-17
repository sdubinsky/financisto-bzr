/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.export.qif;

import android.content.pm.PackageManager;
import android.database.Cursor;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class QifExport extends Export {

    private final DatabaseAdapter db;
    private final WhereFilter filter;
    private final CategoryTree<Category> categories;
    private final HashMap<Long, Category> categoriesMap;

    public QifExport(DatabaseAdapter db, WhereFilter filter) {
        this.db = db;
        this.filter = filter;
        this.categories = db.getAllCategoriesTree(false);
        this.categoriesMap = categories.asMap();
    }

    @Override
    protected void writeHeader(BufferedWriter bw) throws IOException, PackageManager.NameNotFoundException {
        // no header
    }

    @Override
    protected void writeBody(BufferedWriter bw) throws IOException {
        QifBufferedWriter qifWriter = new QifBufferedWriter(bw);
        writeCategories(qifWriter);
        writeAccountsAndTransactions(qifWriter);
    }

    private void writeCategories(QifBufferedWriter qifWriter) throws IOException {
        if (!categories.isEmpty()) {
            qifWriter.writeCategoriesHeader();
            for (Category c : categories) {
                writeCategory(qifWriter, c);
            }
        }
    }

    private void writeCategory(QifBufferedWriter qifWriter, Category c) throws IOException {
        QifCategory qifCategory = QifCategory.fromCategory(c);
        qifCategory.writeTo(qifWriter);
        if (c.hasChildren()) {
            for (Category child : c.children) {
                writeCategory(qifWriter, child);
            }
        }
    }

    private void writeAccountsAndTransactions(QifBufferedWriter qifWriter) throws IOException {
        ArrayList<Account> accounts = db.em().getAllAccountsList();
        for (Account a : accounts) {
            QifAccount qifAccount = writeAccount(qifWriter, a);
            writeTransactionsForAccount(qifWriter, qifAccount, a);
        }
    }

    private QifAccount writeAccount(QifBufferedWriter qifWriter, Account a) throws IOException {
        QifAccount qifAccount = QifAccount.fromAccount(a);
        qifAccount.writeTo(qifWriter);
        return qifAccount;
    }

    private void writeTransactionsForAccount(QifBufferedWriter qifWriter, QifAccount qifAccount, Account account) throws IOException {
        Cursor c = getBlotterForAccount(account);
        try {
            boolean addHeader = true;
            while (c.moveToNext()) {
                if (addHeader) {
                    qifWriter.write("!Type:").write(qifAccount.type).newLine();
                    addHeader = false;
                }
                QifTransaction qifTransaction = QifTransaction.fromBlotterCursor(c);
                qifTransaction.userCategoriesCache(categoriesMap);
                qifTransaction.writeTo(qifWriter);
            }
        } finally {
            c.close();
        }
    }

    private Cursor getBlotterForAccount(Account account) {
        WhereFilter accountFilter = WhereFilter.copyOf(filter);
        accountFilter.put(WhereFilter.Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(account.id)));
        return db.getTransactions(accountFilter);
    }

    @Override
    protected void writeFooter(BufferedWriter bw) throws IOException {
        // no footer
    }

    @Override
    protected String getExtension() {
        return ".qif";
    }

}
