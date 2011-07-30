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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import api.wireless.gdata.docs.client.DocsClient;
import api.wireless.gdata.docs.data.DocumentEntry;
import api.wireless.gdata.parser.ParseException;
import api.wireless.gdata.util.ContentType;
import api.wireless.gdata.util.ServiceException;
import ru.orangesoftware.financisto.db.Database;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.db.DatabaseSchemaEvolution;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.service.RecurrenceScheduler;

import java.io.*;
import java.util.zip.GZIPInputStream;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT;
import static ru.orangesoftware.financisto.backup.Backup.RESTORE_SCRIPTS;

public class DatabaseImport {

	private final Context context;
	private final DatabaseAdapter dbAdapter;
	private final SQLiteDatabase db;
	private final String backupFile;
	private final DatabaseSchemaEvolution schemaEvolution;
	
	public DatabaseImport(Context context, DatabaseAdapter dbAdapter, String backupFile) {
		this.context = context;
		this.dbAdapter = dbAdapter;
		this.db = dbAdapter.db();
		this.backupFile = backupFile;
		this.schemaEvolution = new DatabaseSchemaEvolution(context, Database.DATABASE_NAME, null, Database.DATABASE_VERSION);
	}
	
	public void importDatabase() throws IOException {
		File file = new File(Export.EXPORT_PATH, backupFile);
		FileInputStream inputStream = new FileInputStream(file);
		recoverDatabase(inputStream);
	}
	
	/**
	 * Get backup file from Google docs
	 * 
	 * @param docsClient The Google Docs connection
	 * @param resourceId the key of the recovery document on google docs
	 * @throws ServiceException 
	 * @throws IOException 
	 * @throws ParseException 
	 **/
	public void importOnlineDatabase(DocsClient docsClient, DocumentEntry entry) throws ParseException, IOException, ServiceException {
		InputStream inputStream = docsClient.getFileContent(entry, ContentType.ZIP);
        InputStream in = new BufferedInputStream(new GZIPInputStream(inputStream));
        try {
		    recoverDatabase(in);
        } finally {
		    inputStream.close();
        }
	}
	
	/**
	 * Recover database from a inputStream
	 * 
	 * @param inputStream stream with the backup data
	 **/
	protected void recoverDatabase(InputStream inputStream) throws IOException
	{
		InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
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
            dbAdapter.rebuildRunningBalance();
			scheduleAll();
		} finally {
			br.close();
		}
	}

	/*private void printCurrentSchema() {
		Cursor c = db.rawQuery("SELECT * FROM sqlite_master where type='table'", null);
		try {
			DatabaseUtils.dumpCursor(c);
		} finally {
			c.close();
		}
	}*/

	private void scheduleAll() {
        RecurrenceScheduler scheduler = new RecurrenceScheduler(dbAdapter);
        scheduler.scheduleAll(context);
	}

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
		Cursor c = db.query(V_BLOTTER_FOR_ACCOUNT, new String[]{"SUM("+BlotterColumns.from_amount+")"},
				BlotterColumns.from_account_id +"=?", new String[]{String.valueOf(accountId)},
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
