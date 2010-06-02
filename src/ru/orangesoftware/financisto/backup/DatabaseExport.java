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

import static ru.orangesoftware.financisto.backup.Backup.*;

import java.io.BufferedWriter;
import java.io.IOException;

import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.utils.Utils;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseExport extends Export {
	
	private final Context context;
	private final SQLiteDatabase db;
	
	public DatabaseExport(Context context, SQLiteDatabase db) {
		this.context = context;
		this.db = db;
	}

	@Override
	protected String getExtension() {
		return ".backup";
	}

	@Override
	protected void writeHeader(BufferedWriter bw) throws Exception {
		PackageInfo pi = Utils.getPackageInfo(context);
		bw.write("PACKAGE:");bw.write(pi.packageName);bw.write("\n");
		bw.write("VERSION_CODE:");bw.write(String.valueOf(pi.versionCode));bw.write("\n");
		bw.write("VERSION_NAME:");bw.write(pi.versionName);bw.write("\n");
		bw.write("DATABASE_VERSION:");bw.write(db.getVersion());bw.write("\n");
		bw.write("#START\n");
	}

	@Override
	protected void writeBody(BufferedWriter bw) throws Exception {
		String sql = "SELECT * FROM sqlite_master";
		Cursor c = db.rawQuery(sql, null);
		try {
			while (c.moveToNext()) {
				String tableName = c.getString(c.getColumnIndex("name"));
				if (shouldExportTable(tableName)) {
					exportTable(bw, tableName);
				}
			}
		} finally {
			c.close();
		}
	}

	@Override
	protected void writeFooter(BufferedWriter bw) throws IOException {
		bw.write("#END");
	}

	private void exportTable(BufferedWriter bw, String tableName) throws IOException {
		String sql = "select * from " + tableName + (shouldIgnoreSystemIds(tableName) ? " WHERE _id>=0" : "");
		Cursor c = db.rawQuery(sql, null);
		try {
			String[] columnNames = c.getColumnNames();
			int cols = columnNames.length;
			while (c.moveToNext()) {
				bw.write("$ENTITY:");bw.write(tableName);bw.write("\n");
				for (int i=0; i<cols; i++) {					
					String value = c.getString(i);
					if (!Utils.isEmpty(value)) {
						bw.write(columnNames[i]);bw.write(":");
						bw.write(value);
						bw.write("\n");
					}
				}
				bw.write("$$\n");
			}
		} finally {
			c.close();
		}
	}

	private boolean shouldExportTable(String tableName) {
		for (String table : BACKUP_TABLES) {
			if (table.equalsIgnoreCase(tableName)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean shouldIgnoreSystemIds(String tableName) {
		for (String table : BACKUP_TABLES_WITH_SYSTEM_IDS) {
			if (table.equalsIgnoreCase(tableName)) {
				return true;
			}
		}
		return false;
	}

}
