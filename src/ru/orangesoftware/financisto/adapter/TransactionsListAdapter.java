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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.format.DateUtils;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.adapter.BlotterListAdapter.generateTransactionText;

public class TransactionsListAdapter extends AbstractBlotterListAdapter {
	
	private final Utils u;
    private final StringBuilder sb = new StringBuilder();

	public TransactionsListAdapter(DatabaseAdapter db, Context context, Cursor c) {
		super(db, context, c);
		this.u = new Utils(context);
	}

	@Override
	public void bindView(GenericViewHolder v, Context context, Cursor cursor) {		
		long toAccountId = cursor.getLong(BlotterColumns.TO_ACCOUNT_ID.ordinal());
        String payee = cursor.getString(BlotterColumns.PAYEE.ordinal());
		String note = cursor.getString(BlotterColumns.NOTE.ordinal());
		long locationId = cursor.getLong(BlotterColumns.LOCATION_ID.ordinal());
        String location = "";
        if (locationId > 0) {
            location = cursor.getString(BlotterColumns.LOCATION.ordinal());
        }
		String toAccount = cursor.getString(BlotterColumns.TO_ACCOUNT_TITLE.ordinal());
		long fromAmount = cursor.getLong(BlotterColumns.FROM_AMOUNT.ordinal());
        if (toAccountId > 0) {
			if (fromAmount > 0) {
				note = toAccount+" \u00BB";
				v.lineView.setTextColor(transferColor);
			} else {
				note = "\u00AB "+toAccount;
				v.lineView.setTextColor(transferColor);
			}	
		} else {
			v.lineView.setTextColor(Color.WHITE);
		}
		
		long categoryId = cursor.getLong(BlotterColumns.CATEGORY_ID.ordinal());
        String category = "";
		if (categoryId > 0) {
            category = cursor.getString(BlotterColumns.CATEGORY_TITLE.ordinal());
		}
        String text = generateTransactionText(sb, payee, note, location, category);
        v.lineView.setText(text);

		long currencyId = cursor.getLong(BlotterColumns.FROM_ACCOUNT_CURRENCY_ID.ordinal());
		Currency c = CurrencyCache.getCurrency(currencyId);
		u.setAmountText(v.amountView, c, fromAmount, true);
		if (fromAmount > 0) {
			v.iconView.setImageDrawable(icBlotterIncome);
		} else if (fromAmount < 0) {
			v.iconView.setImageDrawable(icBlotterExpense);
		}
		
		long date = cursor.getLong(BlotterColumns.DATETIME.ordinal());
		v.numberView.setText(DateUtils.formatDateTime(context, date, 
				DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_ABBREV_MONTH));
		if (date > System.currentTimeMillis()) {
			v.numberView.setTextColor(futureColor);
		} else {
			v.numberView.setTextColor(v.labelView.getTextColors().getDefaultColor());
		}

	}

}
