/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.qif;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/11/11 10:32 PM
 */
public class QifParser {

    private final QifBufferedReader r;

    public final List<QifAccount> accounts = new ArrayList<QifAccount>();
    public final List<QifCategory> categories = new ArrayList<QifCategory>();
    public final Set<String> payees = new HashSet<String>();

    public QifParser(QifBufferedReader r) {
        this.r = r;
    }

    public void parse() throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("!Account")) {
                parseAccount();
            } else if (line.startsWith("!Type:Cat")) {
                parseCategories();
            }
        }
    }

    private void parseCategories() throws IOException {
        while (true) {
            QifCategory category = new QifCategory();
            category.readFrom(r);
            categories.add(category);
            if (shouldBreakCurrentBlock()) {
                break;
            }
        }
    }

    private void parseAccount() throws IOException {
        QifAccount account = new QifAccount();
        account.readFrom(r);
        accounts.add(account);
        String peek = r.peekLine();
        if (peek != null) {
            if (peek.startsWith("!Type:")) {
                r.readLine();
                while (true) {
                    QifTransaction t = new QifTransaction();
                    t.readFrom(r);
                    addPayeeFromTransaction(t);
                    account.transactions.add(t);
                    if (shouldBreakCurrentBlock()) {
                        break;
                    }
                }
            }
        }
    }

    private void addPayeeFromTransaction(QifTransaction t) {
        if (isNotEmpty(t.payee)) {
            payees.add(t.payee);
        }
    }

    private boolean shouldBreakCurrentBlock() throws IOException {
        String peek = r.peekLine();
        return peek == null || peek.startsWith("!");
    }

}
