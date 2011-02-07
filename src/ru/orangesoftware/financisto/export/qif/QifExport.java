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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class QifExport extends Export {

    private final DatabaseAdapter db;

    public QifExport(DatabaseAdapter db) {
        this.db = db;
    }

    @Override
    protected void writeHeader(BufferedWriter bw) throws IOException, PackageManager.NameNotFoundException {
        // no header
    }

    @Override
    protected void writeBody(BufferedWriter bw) throws IOException {
        QifBufferedWriter qifWriter = new QifBufferedWriter(bw);
        ArrayList<Account> accounts = db.em().getAllAccountsList();
        for (Account a : accounts) {
            QifAccount qifAccount = writeAccount(qifWriter, a);
            writeTransactionsForAccount(qifWriter, qifAccount, WhereFilter.empty().eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(a.id)));
        }
    }

    private QifAccount writeAccount(QifBufferedWriter qifWriter, Account a) throws IOException {
        QifAccount qifAccount = QifAccount.fromAccount(a);
        qifAccount.writeTo(qifWriter);
        return qifAccount;
    }

    private void writeTransactionsForAccount(QifBufferedWriter qifWriter, QifAccount qifAccount, WhereFilter filter) throws IOException {
        Cursor c = db.getBlotter(filter);
        try {
            boolean addHeader = true;
            while (c.moveToNext()) {
                if (addHeader) {
                    qifWriter.write("!Type:").write(qifAccount.type).newLine();
                    addHeader = false;
                }
                QifTransaction qifTransaction = QifTransaction.fromBlotterCursor(c);
                qifTransaction.writeTo(qifWriter);
            }
        } finally {
            c.close();
        }
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
