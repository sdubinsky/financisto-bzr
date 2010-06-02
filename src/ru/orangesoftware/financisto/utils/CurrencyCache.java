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
package ru.orangesoftware.financisto.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.ConcurrentHashMap;

import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.orb.EntityManager;
import ru.orangesoftware.orb.Query;
import android.database.Cursor;

public class CurrencyCache {

	private static volatile ConcurrentHashMap<Long, Currency> CURRENCIES = new ConcurrentHashMap<Long, Currency>();	
	
	public static Currency getCurrency(long currencyId) {
		return CURRENCIES.get(currencyId);
	}
	
	public static Currency putCurrency(Currency currency) {
		Currency c = CURRENCIES.putIfAbsent(currency.id, currency);
		if (c == null) {
			c = currency;
		}
		return c;
	}
	
	public static void initialize(EntityManager em) {
		ConcurrentHashMap<Long, Currency> currencies = new ConcurrentHashMap<Long, Currency>();
		Query<Currency> q = em.createQuery(Currency.class);
		Cursor c = q.execute();
		try {
			while (c.moveToNext()) {
				Currency currency = EntityManager.loadFromCursor(c, Currency.class);
				currencies.put(currency.id, currency);
			}
		} finally {
			c.close();
		}
		CURRENCIES = currencies;
	}
	
	public static DecimalFormat createCurrencyFormat(Currency c) {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator(charOrEmpty(c.decimalSeparator, dfs.getDecimalSeparator()));
		dfs.setGroupingSeparator(charOrEmpty(c.groupSeparator, dfs.getGroupingSeparator()));
		dfs.setMonetaryDecimalSeparator(dfs.getDecimalSeparator());
		dfs.setCurrencySymbol(c.symbol);

		DecimalFormat df = new DecimalFormat("#,##0.00", dfs);
		df.setGroupingUsed(dfs.getGroupingSeparator() > 0);
		df.setMinimumFractionDigits(c.decimals);
		df.setMaximumFractionDigits(c.decimals);
		df.setDecimalSeparatorAlwaysShown(false);
		return df;
	}

	private static char charOrEmpty(String s, char c) {
		return s != null ? (s.length() > 2 ? s.charAt(1) : 0): c;
	}


}
