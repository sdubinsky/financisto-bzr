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
package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.view.NodeInflater;

import java.util.ArrayList;

public class QifExportActivity extends AbstractExportActivity implements ActivityLayoutListener {

    public static final String SELECTED_ACCOUNTS = "SELECTED_ACCOUNTS";

    private DatabaseAdapter db;
    private ArrayList<Account> accounts;
    private ArrayList<Account> selectedAccounts;

    private Button bAccounts;

    public QifExportActivity() {
        super(R.layout.qif_export);
    }

    @Override
    protected void internalOnCreate() {
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater nodeInflater = new NodeInflater(layoutInflater);
        final ActivityLayout activityLayout = new ActivityLayout(nodeInflater, this);

        db = new DatabaseAdapter(this);
        db.open();

        accounts = db.em().getAllAccountsList();

        bAccounts = (Button)findViewById(R.id.bAccounts);
        bAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityLayout.selectMultiChoice(QifExportActivity.this, R.id.bAccounts, R.string.accounts, accounts);
            }
        });

        clearFilter();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onSelected(int id, ArrayList<? extends MultiChoiceItem> items) {
        selectedAccounts = getSelectedAccounts(items);
        if (selectedAccounts.size() == items.size()) {
            bAccounts.setText(R.string.all_accounts);
        } else {
            StringBuilder sb = new StringBuilder();
            for (Account a : selectedAccounts) {
                appendItemTo(sb, a.title);
            }
            bAccounts.setText(sb.toString());
        }
    }

    private ArrayList<Account> getSelectedAccounts(ArrayList<? extends MultiChoiceItem> items) {
        ArrayList<Account> selected = new ArrayList<Account>();
        for (MultiChoiceItem i : items) {
            if (i.isChecked()) {
                selected.add((Account)i);
            }
        }
        return selected;
    }

    private void appendItemTo(StringBuilder sb, String s) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(s);
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        long[] selectedIds = getSelectedAccountsIds();
        if (selectedIds.length > 0) {
            data.putExtra(SELECTED_ACCOUNTS, selectedIds);
        }
    }

    private long[] getSelectedAccountsIds() {
        if (selectedAccounts != null) {
            int count = selectedAccounts.size();
            long[] ids = new long[count];
            for (int i=0; i<count; i++) {
                ids[i] = selectedAccounts.get(i).id;
            }
            return ids;
        }
        return new long[0];
    }

}
