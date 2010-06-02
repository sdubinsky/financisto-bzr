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
package ru.orangesoftware.financisto.export;

import static ru.orangesoftware.financisto.utils.DateUtils.FORMAT_DATE_ISO_8601;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;

import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.model.CategoriesTree;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;
import android.database.Cursor;

public class CSVExport extends Export {

	private final DatabaseAdapter db;
	private final WhereFilter filter;
	private final NumberFormat f; 
	
	public CSVExport(DatabaseAdapter db, WhereFilter filter, Currency currency) {
		this.db = db;
		this.filter = filter;
		this.f = CurrencyCache.createCurrencyFormat(currency);
	}
	
	@Override
	protected String getExtension() {
		return ".csv";
	}

	@Override
	protected void writeHeader(BufferedWriter bw) throws Exception {
		bw.write("date/time,account,amount,currency,category,parent,location,note\n");
	}

	@Override
	protected void writeBody(BufferedWriter bw) throws Exception {
		Csv.Writer w = new Csv.Writer(bw).delimiter(',');
		try {
			CategoriesTree categories = new CategoriesTree(db);
			Cursor c = db.getBlotter(filter);
			try {			
				StringBuilder sb = new StringBuilder();
				while (c.moveToNext()) {
					writeLine(w, c, categories, sb);			
				}					
			} finally {
				c.close();
			}
		} finally {
			w.close();
		}
	}

	private void writeLine(Csv.Writer w, Cursor cursor, CategoriesTree categories, StringBuilder sb) {
		long date = cursor.getLong(BlotterColumns.Indicies.DATETIME);
		Date dt = new Date(date);
		long categoryId = cursor.getLong(BlotterColumns.Indicies.CATEGORY_ID);
		Category category = categories.getById(categoryId);
		long toAccountId = cursor.getLong(BlotterColumns.Indicies.TO_ACCOUNT_ID);
		if (toAccountId > 0) {
			String fromAccountTitle = cursor.getString(BlotterColumns.Indicies.FROM_ACCOUNT_TITLE);
			String toAccountTitle = cursor.getString(BlotterColumns.Indicies.TO_ACCOUNT_TITLE);
			long fromCurrencyId = cursor.getLong(BlotterColumns.Indicies.FROM_ACCOUNT_CURRENCY_ID);
			long toCurrencyId = cursor.getLong(BlotterColumns.Indicies.TO_ACCOUNT_CURRENCY_ID);
			long fromAmount = cursor.getLong(BlotterColumns.Indicies.FROM_AMOUNT);
			long toAmount = cursor.getLong(BlotterColumns.Indicies.TO_AMOUNT);
			String note = cursor.getString(BlotterColumns.Indicies.NOTE);
			writeLine(w, dt, fromAccountTitle, fromAmount, fromCurrencyId, category, "Transfer Out", note);
			writeLine(w, dt, toAccountTitle, toAmount, toCurrencyId, category, "Transfer In", note);
		} else {
			String fromAccountTitle = cursor.getString(BlotterColumns.Indicies.FROM_ACCOUNT_TITLE);
			String note = cursor.getString(BlotterColumns.Indicies.NOTE);
			String location = cursor.getString(BlotterColumns.Indicies.LOCATION);
			long fromCurrencyId = cursor.getLong(BlotterColumns.Indicies.FROM_ACCOUNT_CURRENCY_ID);
			long amount = cursor.getLong(BlotterColumns.Indicies.FROM_AMOUNT);
			writeLine(w, dt, fromAccountTitle, amount, fromCurrencyId, category, location, note);
		}
	}
	
	private void writeLine(Csv.Writer w, Date dt, String account, long amount, long currencyId, Category category, String location, String note) {
		w.value(FORMAT_DATE_ISO_8601.format(dt));
		w.value(account);
		w.value(f.format(new BigDecimal(amount).divide(Utils.HUNDRED)));
		Currency c = CurrencyCache.getCurrency(currencyId);
		w.value(c.name);
		w.value(category != null ? category.title : "");
		w.value(category != null ? (category.parent != null ? category.parent.title : "") : "");
		w.value(location);
		w.value(note);
		w.newLine();
	}

	@Override
	protected void writeFooter(BufferedWriter bw) throws Exception {
	}

}
