/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.utils.CurrencyExportPreferences;

import java.util.ArrayList;
import java.util.List;


public class CsvImportActivity extends AbstractImportActivity implements ActivityLayoutListener {

    public static final String CSV_IMPORT_SELECTED_ACCOUNT = "CSV_IMPORT_SELECTED_ACCOUNT";
    public static final String CSV_IMPORT_DATE_FORMAT = "CSV_IMPORT_DATE_FORMAT";
    public static final String CSV_IMPORT_FILENAME = "CSV_IMPORT_FILENAME";
    public static final String CSV_IMPORT_FIELD_SEPARATOR = "CSV_IMPORT_FIELD_SEPARATOR";

    private final CurrencyExportPreferences currencyPreferences = new CurrencyExportPreferences("csv");

    private DatabaseAdapter db;
    private List<Account> accounts;
    private int checkedAccount = 1;
    private Button bAccounts;

    public CsvImportActivity() {
        super(R.layout.csv_import);
    }

    @Override
    protected void internalOnCreate() {
        db = new DatabaseAdapter(this);
        db.open();

        accounts = db.em().getAllAccountsList();

        bAccounts = (Button) findViewById(R.id.bAccounts);
        bAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int count = accounts.size();
                String[] accountsTitles = new String[count];
                for (int i = 0; i < count; i++) {
                    accountsTitles[i] = accounts.get(i).getTitle();
                    if (accounts.get(i).isChecked()) {
                        checkedAccount = i;
                    }
                }

                new AlertDialog.Builder(CsvImportActivity.this)
                        .setTitle(R.string.accounts)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                for (int i = 0; i < accounts.size(); i++) {
                                    if (i == checkedAccount) {
                                        accounts.get(i).setChecked(true);
                                    } else
                                        accounts.get(i).setChecked(false);
                                }
                                bAccounts.setText(accounts.get(checkedAccount).getTitle());
                                savePreferences();
                            }

                        })
                        .setSingleChoiceItems(accountsTitles, checkedAccount, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                checkedAccount = which;
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();

            }
        });

        Button bOk = (Button) findViewById(R.id.bOK);
        bOk.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (edFilename.getText().toString().equals("")) {
                    Toast.makeText(CsvImportActivity.this, R.string.select_filename, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (checkedAccount == -1) {
                    Toast.makeText(CsvImportActivity.this, R.string.choose_account, Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent data = new Intent();
                updateResultIntentFromUi(data);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        Button bCancel = (Button) findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

    }


    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onSelected(int id, List<? extends MultiChoiceItem> items) {
        List<Account> selectedAccounts = getSelectedAccounts();
        if (selectedAccounts.size() == 0 || selectedAccounts.size() == accounts.size()) {
            bAccounts.setText(R.string.choose_account);
            checkedAccount = -1;
        } else {
            StringBuilder sb = new StringBuilder();
            for (Account a : selectedAccounts) {
                appendItemTo(sb, a.title);
                checkedAccount = 1;
            }
            bAccounts.setText(sb.toString());
        }
    }

    private ArrayList<Account> getSelectedAccounts() {
        ArrayList<Account> selected = new ArrayList<Account>();
        for (MultiChoiceItem i : accounts) {
            if (i.isChecked()) {
                selected.add((Account) i);
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
        currencyPreferences.updateIntentFromUI(this, data);
        long[] selectedIds = getSelectedAccountsIds();
        if (selectedIds.length > 0) {
            data.putExtra(CSV_IMPORT_SELECTED_ACCOUNT, selectedIds);
        }
        Spinner dateFormats = (Spinner) findViewById(R.id.spinnerDateFormats);
        data.putExtra(CSV_IMPORT_DATE_FORMAT, dateFormats.getSelectedItem().toString());
        data.putExtra(CSV_IMPORT_FILENAME, edFilename.getText().toString());
        Spinner fieldSeparator = (Spinner) findViewById(R.id.spinnerFieldSeparator);
        data.putExtra(CSV_IMPORT_FIELD_SEPARATOR, fieldSeparator.getSelectedItem().toString().charAt(1));

    }

    private long[] getSelectedAccountsIds() {
        List<Long> selectedAccounts = new ArrayList<Long>(accounts.size());
        for (Account account : accounts) {
            if (account.isChecked()) {
                selectedAccounts.add(account.id);
            }
        }
        int count = selectedAccounts.size();
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = selectedAccounts.get(i);
        }
        return ids;
    }

    @Override
    protected void savePreferences() {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        currencyPreferences.savePreferences(this, editor);

        long[] selectedIds = getSelectedAccountsIds();
        if (selectedIds.length > 0) {
            editor.putString(CSV_IMPORT_SELECTED_ACCOUNT, joinSelectedAccounts(selectedIds));
        }

        Spinner dateFormats = (Spinner) findViewById(R.id.spinnerDateFormats);
        editor.putInt(CSV_IMPORT_DATE_FORMAT, dateFormats.getSelectedItemPosition());
        editor.putString(CSV_IMPORT_FILENAME, edFilename.getText().toString());
        Spinner fieldSeparator = (Spinner) findViewById(R.id.spinnerFieldSeparator);
        editor.putInt(CSV_IMPORT_FIELD_SEPARATOR, fieldSeparator.getSelectedItemPosition());
        editor.commit();
    }

    private String joinSelectedAccounts(long[] selectedIds) {
        StringBuilder sb = new StringBuilder();
        for (long selectedId : selectedIds) {
            if (sb.length() > 0) sb.append(",");
            sb.append(selectedId);
        }
        return sb.toString();
    }

    @Override
    protected void restorePreferences() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        currencyPreferences.restorePreferences(this, preferences);

        String selectedIds = preferences.getString(CSV_IMPORT_SELECTED_ACCOUNT, "");
        parseSelectedAccounts(selectedIds);
        onSelected(-1, accounts);

        Spinner dateFormats = (Spinner) findViewById(R.id.spinnerDateFormats);
        dateFormats.setSelection(preferences.getInt(CSV_IMPORT_DATE_FORMAT, 0));
        edFilename = (EditText) findViewById(R.id.edFilename);
        edFilename.setText(preferences.getString(CSV_IMPORT_FILENAME, ""));
        Spinner fieldSeparator = (Spinner) findViewById(R.id.spinnerFieldSeparator);
        fieldSeparator.setSelection(preferences.getInt(CSV_IMPORT_FIELD_SEPARATOR, 0));
    }

    private void parseSelectedAccounts(String selectedIds) {
        try {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(selectedIds);
            for (String s : splitter) {
                long id = Long.parseLong(s);
                for (Account account : accounts) {
                    if (account.id == id) {
                        account.setChecked(true);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            // ignore
        }
    }


}
