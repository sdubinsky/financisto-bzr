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
package ru.orangesoftware.financisto.backup;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import ru.orangesoftware.financisto.db.Database;
import ru.orangesoftware.financisto.db.DatabaseSchemaEvolution;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.service.FinancistoService;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DatabaseImport {

	private final Context context;
	private final SQLiteDatabase db;
	private final String backupFile;
	private final DatabaseSchemaEvolution schemaEvolution;
	
	public DatabaseImport(Context context, SQLiteDatabase db, String backupFile) {
		this.context = context;
		this.db = db;
		this.backupFile = backupFile;
		this.schemaEvolution = new DatabaseSchemaEvolution(context, Database.DATABASE_NAME, null, Database.DATABASE_VERSION);
	}
	
	public void importDatabase() throws IOException {
		File file = new File(Export.EXPORT_PATH, backupFile);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8");
		BufferedReader br = new BufferedReader(isr, 65535);
		try {
			db.beginTransaction();
			try {
				for (String tableName : Backup.BACKUP_TABLES) {
					db.execSQL("delete from "+tableName);
				}
				//printCurrentSchema();
				boolean insideEntity = false;
				ContentValues values = new ContentValues();
				String line;
				String tableName = null;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("$")) {
						if ("$$".equals(line)) {
							if (tableName != null && values.size() > 0) {
								db.insert(tableName, null, values);
								tableName = null;								
								insideEntity = false;
							}
						} else {
							int i = line.indexOf(":");
							if (i > 0) {
								tableName = line.substring(i+1);
								insideEntity = true;
								values.clear();
							}
						}						
					} else {
						if (insideEntity) {
							int i = line.indexOf(":");
							if (i > 0) {
								String columnName = line.substring(0, i);
								String value = line.substring(i+1);
								values.put(columnName, value);
							}							
						}
					}
				}
				runRestoreAlterscripts();
				recalculateAccountsBalances();
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			scheduleAll();
		} finally {
			br.close();
		}
	}

	private void printCurrentSchema() {
		Cursor c = db.rawQuery("SELECT * FROM sqlite_master where type='table'", null);
		try {
			DatabaseUtils.dumpCursor(c);
		} finally {
			c.close();
		}
	}

	private void scheduleAll() {
		FinancistoService.scheduleAll(context, new MyEntityManager(context, db));
	}

	private static final String[] RESTORE_SCRIPTS = {
		"20100114_1158_alter_accounts_types.sql",
		"20100511_2253_add_delete_after_expired_attribute.sql"
	};
	
	private void runRestoreAlterscripts() throws IOException {
		for (String script : RESTORE_SCRIPTS) {
			schemaEvolution.runAlterScript(db, script);
		}
	}

	private void recalculateAccountsBalances() {
		Cursor accountsCursor = db.query(ACCOUNT_TABLE, new String[]{AccountColumns.ID}, null, null, null, null, null);
		try {
			while (accountsCursor.moveToNext()) {
				long accountId = accountsCursor.getLong(0);
				recalculateAccountsBalances(accountId);
			}			
		} finally {
			accountsCursor.close();
		}
	}

	private void recalculateAccountsBalances(long accountId) {
		Cursor c = db.query(V_BLOTTER_FOR_ACCOUNT, new String[]{"SUM("+BlotterColumns.FROM_AMOUNT+")"}, 
				BlotterColumns.FROM_ACCOUNT_ID+"=?", new String[]{String.valueOf(accountId)}, 
				null, null, null);
		try {	
			long amount = 0;
			if (c.moveToFirst()) {
				amount = c.getLong(0);
			}
			ContentValues values = new ContentValues();
			values.put(AccountColumns.TOTAL_AMOUNT, amount);
			db.update(ACCOUNT_TABLE, values, AccountColumns.ID+"=?", new String[]{String.valueOf(accountId)});
			Log.i("DatabaseImport", "Recalculating amount for "+accountId);
		} finally {
			c.close();
		}
	}
	
}
