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
package ru.orangesoftware.financisto.export.csv;

import android.database.Cursor;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Map;

import static ru.orangesoftware.financisto.utils.DateUtils.FORMAT_DATE_ISO_8601;
import static ru.orangesoftware.financisto.utils.DateUtils.FORMAT_TIME_ISO_8601;

public class CsvExport extends Export {

	private final DatabaseAdapter db;
    private final CsvExportOptions options;

	public CsvExport(DatabaseAdapter db, CsvExportOptions options) {
		this.db = db;
		this.options = options;
	}
	
	@Override
	protected String getExtension() {
		return ".csv";
	}

	@Override
	protected void writeHeader(BufferedWriter bw) throws IOException  {
        if (options.writeUtfBom) {
            byte[] bom = new byte[3];
            bom[0] = (byte) 0xEF;
            bom[1] = (byte) 0xBB;
            bom[2] = (byte) 0xBF;
            bw.write(new String(bom,"UTF-8"));
        }
		if (options.includeHeader) {
			Csv.Writer w = new Csv.Writer(bw).delimiter(options.fieldSeparator);
			w.value("date").value("time").value("account").value("amount").value("currency");
			w.value("category").value("parent").value("payee").value("location").value("project").value("note");
			w.newLine();
		}
	}

	@Override
	protected void writeBody(BufferedWriter bw) throws IOException {
		Csv.Writer w = new Csv.Writer(bw).delimiter(options.fieldSeparator);
		try {
			Map<Long, Category> categoriesMap = db.getCategoriesMap(false);
			Cursor c = db.getBlotter(options.filter);
			try {			
				while (c.moveToNext()) {
					writeLine(w, c, categoriesMap);
				}					
			} finally {
				c.close();
			}
		} finally {
			w.close();
		}
	}

	private void writeLine(Csv.Writer w, Cursor cursor, Map<Long, Category> categoriesMap) {
		long date = cursor.getLong(BlotterColumns.datetime.ordinal());
		Date dt = new Date(date);
		long categoryId = cursor.getLong(BlotterColumns.category_id.ordinal());
		Category category = getCategoryById(categoriesMap, categoryId);
		long toAccountId = cursor.getLong(BlotterColumns.to_account_id.ordinal());
		String project = cursor.getString(BlotterColumns.project.ordinal());
		if (toAccountId > 0) {
			String fromAccountTitle = cursor.getString(BlotterColumns.from_account_title.ordinal());
			String toAccountTitle = cursor.getString(BlotterColumns.to_account_title.ordinal());
			long fromCurrencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
			long toCurrencyId = cursor.getLong(BlotterColumns.to_account_currency_id.ordinal());
			long fromAmount = cursor.getLong(BlotterColumns.from_amount.ordinal());
			long toAmount = cursor.getLong(BlotterColumns.to_amount.ordinal());
			String note = cursor.getString(BlotterColumns.note.ordinal());
			writeLine(w, dt, fromAccountTitle, fromAmount, fromCurrencyId, category, "", "Transfer Out", project, note);
			writeLine(w, dt, toAccountTitle, toAmount, toCurrencyId, category, "", "Transfer In", project, note);
		} else {
			String fromAccountTitle = cursor.getString(BlotterColumns.from_account_title.ordinal());
			String note = cursor.getString(BlotterColumns.note.ordinal());
			String location = cursor.getString(BlotterColumns.location.ordinal());
			long fromCurrencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
			long amount = cursor.getLong(BlotterColumns.from_amount.ordinal());
            String payee = cursor.getString(BlotterColumns.payee.ordinal());
			writeLine(w, dt, fromAccountTitle, amount, fromCurrencyId, category, payee, location, project, note);
		}
	}
	
	private void writeLine(Csv.Writer w, Date dt, String account, long amount, long currencyId, 
			Category category, String payee, String location, String project, String note) {
		w.value(FORMAT_DATE_ISO_8601.format(dt));
		w.value(FORMAT_TIME_ISO_8601.format(dt));
		w.value(account);
		w.value(options.amountFormat.format(new BigDecimal(amount).divide(Utils.HUNDRED)));
		Currency c = CurrencyCache.getCurrency(currencyId);
		w.value(c.name);
		w.value(category != null ? category.title : "");
		String sParent = buildPath(category);
		w.value(sParent);
        w.value(payee);
		w.value(location);
		w.value(project);
		w.value(note);
		w.newLine();
	}

	private String buildPath(Category category) {
		if (category == null || category.parent == null) {
			return "";
		} else {
            StringBuilder sb = new StringBuilder(category.parent.title);
			for (Category cat = category.parent.parent; cat != null; cat = cat.parent) {
                sb.insert(0,":").insert(0, cat.title);
			}
			return sb.toString();
		}
	}

	@Override
	protected void writeFooter(BufferedWriter bw) throws IOException {
	}

	public Category getCategoryById(Map<Long, Category> categoriesMap, long id) {
		return categoriesMap.get(id);
	}
	
}
