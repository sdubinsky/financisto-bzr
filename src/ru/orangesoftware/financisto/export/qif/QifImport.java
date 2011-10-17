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
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;

import java.io.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 9/25/11 9:54 PM
 */
public class QifImport {

    private final DatabaseAdapter db;
    private final MyEntityManager em;

    public QifImport(DatabaseAdapter db) {
        this.db = db;
        this.em = db.em();
    }

    public void doImport(InputStream in) throws IOException {
        try {
            QifBufferedReader r = new QifBufferedReader(new BufferedReader(new InputStreamReader(in, "UTF-8")));
            //r.readEverything();
            //insertAccounts(r.accounts);
            //insertTransactions(r.transactions);
        } finally {
            in.close();
        }
    }

    private void insertAccounts(List<QifAccount> accounts) {
        for (QifAccount account : accounts) {
            Account a = account.toAccount();
            em.saveAccount(a);
        }
    }

    private void insertTransactions(List<QifTransaction> transactions) {
        for (QifTransaction transaction : transactions) {
            Transaction t = transaction.toTransaction();
            db.insertOrUpdate(t);
        }
    }

}
