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
package ru.orangesoftware.financisto.report;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import ru.orangesoftware.financisto.activity.BlotterActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.graph.GraphStyle;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public abstract class AbstractReport implements Report {
	
	protected static final GraphStyle DEFAULT_STYLE = new GraphStyle.Builder().build();
	
	protected final Context context;
	protected final boolean includeTransfers;
	
	public AbstractReport(Context context) {
		this.context = context;
		this.includeTransfers = MyPreferences.isIncludeTransfersIntoReports(context); 
	}
	
	protected String alterName(long id, String name) {
		return name;
	}
	
	protected ArrayList<GraphUnit> queryReport(DatabaseAdapter db, String table, WhereFilter filter) {
		filterTransfers(filter);
		Cursor c = db.db().query(table, DatabaseHelper.ReportColumns.NORMAL_PROJECTION,  
				filter.getSelection(), filter.getSelectionArgs(), null, null, "_id");
		return getUnitsFromCursorAndSort(c);
	}

	protected void filterTransfers(WhereFilter filter) {
		if (!includeTransfers) {
			filter.put(Criteria.eq(ReportColumns.IS_TRANSFER, "0"));
		}
	}

	protected ArrayList<GraphUnit> getUnitsFromCursor(Cursor c) {
		return getUnitsFromCursor(c, false);
	}
	
	protected ArrayList<GraphUnit> getUnitsFromCursorAndSort(Cursor c) {
		return getUnitsFromCursor(c, true);
	}
	
	private ArrayList<GraphUnit> getUnitsFromCursor(Cursor c, boolean sort) {
		try {
			Set<GraphUnit> units = sort ? new TreeSet<GraphUnit>() : new LinkedHashSet<GraphUnit>();
			GraphUnit u = null;
			long lastId = -1;
			while (c.moveToNext()) {
				long id = getId(c);
				String name = c.getString(ReportColumns.Indicies.NAME);
				long currencyId = c.getLong(ReportColumns.Indicies.CURRENCY_ID);
				long amount = c.getLong(ReportColumns.Indicies.AMOUNT);	
				if (id != lastId) {
					if (u != null) {
						units.add(u);
					}
					u = new GraphUnit(id, alterName(id, name), DEFAULT_STYLE);
					lastId = id;
				}
				Currency currency = CurrencyCache.getCurrencyOrEmpty(currencyId);
				u.addAmount(currency, amount);
			}
			if (u != null) {
				units.add(u);
			}
			return new ArrayList<GraphUnit>(units);
		} finally {
			c.close();
		}
	}
	
	protected GraphUnit getUnitFromCursor(Cursor c, long id) {
		try {
			GraphUnit u = new GraphUnit(id, alterName(id, null), DEFAULT_STYLE);
			while (c.moveToNext()) {				
				long currencyId = c.getLong(2);
				long amount = c.getLong(3);
				Currency currency = CurrencyCache.getCurrencyOrEmpty(currencyId);
				u.addAmount(currency, amount);
			}
			return u;
		} finally {
			c.close();
		}
	}

	protected long getId(Cursor c) {
		return c.getLong(0);
	}

	@Override
	public Intent createActivityIntent(Context context, DatabaseAdapter db, WhereFilter parentFilter, long id) {
		WhereFilter filter = WhereFilter.empty();
		Criteria c = parentFilter.get(BlotterFilter.DATETIME);
		if (c != null) {
			filter.put(c);
		}
		c = getCriteriaForId(db, id);
		if (c != null) {
			filter.put(c);
		}
		Intent intent = new Intent(context, BlotterActivity.class);
		filter.toIntent(intent);
		return intent;
	}

}
