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
package ru.orangesoftware.financisto.adapter;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.format.DateUtils;

public class TransactionsListAdapter extends AbstractBlotterListAdapter {
	
	public TransactionsListAdapter(DatabaseAdapter db, Context context, Cursor c) {
		super(db, context, c);
	}

	@Override
	public void bindView(GenericViewHolder v, Context context, Cursor cursor) {		
		long toAccountId = cursor.getLong(BlotterColumns.Indicies.TO_ACCOUNT_ID);
		String note = cursor.getString(BlotterColumns.Indicies.NOTE);
		String location = cursor.getString(BlotterColumns.Indicies.LOCATION);
		long locationId = cursor.getLong(BlotterColumns.Indicies.LOCATION_ID);
		if (locationId > 0 && location != null && location.length() > 0) {
			note = location+(Utils.isNotEmpty(note) ? ": "+note : "");
		}

		String toAccount = cursor.getString(BlotterColumns.Indicies.TO_ACCOUNT_TITLE);
		long fromAmount = cursor.getLong(BlotterColumns.Indicies.FROM_AMOUNT);
		long amount = fromAmount;
		if (toAccountId > 0) {
			if (fromAmount > 0) {
				note = toAccount+" »";
				v.lineView.setTextColor(transferColor);
			} else {
				note = "« "+toAccount;
				v.lineView.setTextColor(transferColor);
			}	
		} else {
			v.lineView.setTextColor(Color.WHITE);
		}
		
		long categoryId = cursor.getLong(BlotterColumns.Indicies.CATEGORY_ID);
		if (categoryId > 0) {
			String categoryTitle = cursor.getString(BlotterColumns.Indicies.CATEGORY_TITLE);
			if (note != null && note.length() > 0) {
				v.lineView.setText(categoryTitle+" ("+note+")");
			} else {
				v.lineView.setText(categoryTitle);
			}
		} else {
			v.lineView.setText(note);
		}		
		
		long currencyId = cursor.getLong(BlotterColumns.Indicies.FROM_ACCOUNT_CURRENCY_ID);
		Currency c = CurrencyCache.getCurrency(currencyId);
		u.setAmountText(v.amountView, c, amount, true);
		if (amount > 0) {
			v.iconView.setImageDrawable(icBlotterIncome);
		} else if (amount < 0) {
			v.iconView.setImageDrawable(icBlotterExpense);
		}
		
		long date = cursor.getLong(BlotterColumns.Indicies.DATETIME);
		v.numberView.setText(DateUtils.formatDateTime(context, date, 
				DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_ABBREV_MONTH));
		if (date > System.currentTimeMillis()) {
			v.numberView.setTextColor(futureColor);
		} else {
			v.numberView.setTextColor(v.labelView.getTextColors().getDefaultColor());
		}

	}

}
