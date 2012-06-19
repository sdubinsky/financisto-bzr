/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import ru.orangesoftware.financisto.R;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 6/12/12 11:14 PM
 */
public class PurgeAccountActivity extends AbstractActivity {

    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purge_account);

        layout = (LinearLayout)findViewById(R.id.layout);
        createNodes();
    }

    private void createNodes() {
        x.addCheckboxNode(layout, R.id.backup, R.string.database_backup, R.string.purge_account_backup_database, true);
        x.addListNode(layout, R.id.date, R.string.date, "?");
    }

    @Override
    protected void onClick(View v, int id) {
    }

}
