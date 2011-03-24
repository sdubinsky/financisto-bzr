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

import java.util.*;

import ru.orangesoftware.financisto.activity.BlotterActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.graph.Amount;
import ru.orangesoftware.financisto.graph.GraphStyle;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public abstract class AbstractReport implements Report {
	
	public static final GraphStyle DEFAULT_STYLE = new GraphStyle.Builder().build();
	
	protected final Context context;
	protected final boolean includeTransfers;
	
	public AbstractReport(Context context) {
		this.context = context;
		this.includeTransfers = MyPreferences.isIncludeTransfersIntoReports(context); 
	}
	
	protected String alterName(long id, String name) {
		return name;
	}
	
	protected ReportData queryReport(DatabaseAdapter db, String table, WhereFilter filter) {
		filterTransfers(filter);
		Cursor c = db.db().query(table, DatabaseHelper.ReportColumns.NORMAL_PROJECTION,
                filter.getSelection(), filter.getSelectionArgs(), null, null, "_id");
		ArrayList<GraphUnit> units = getUnitsFromCursor(c);
        Total[] totals = calculateTotals(units);
        return new ReportData(units, totals);
	}

    protected void filterTransfers(WhereFilter filter) {
		if (!includeTransfers) {
			filter.put(Criteria.eq(ReportColumns.IS_TRANSFER, "0"));
		}
	}

	private ArrayList<GraphUnit> getUnitsFromCursor(Cursor c) {
		try {
			ArrayList<GraphUnit> units = new ArrayList<GraphUnit>();
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
            for (GraphUnit unit : units) {
                unit.calculateMaxAmount();
            }
            Collections.sort(units);
			return units;
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

    protected Total[] calculateTotals(ArrayList<? extends GraphUnit> units) {
        HashMap<Long, Total> map = new HashMap<Long, Total>();
        for (GraphUnit u : units) {
            for (Amount a : u.amounts.values()) {
                Total t = getOrCreate(map, a.currency);
                long amount = a.amount;
                if (amount > 0) {
                    t.amount += amount;
                } else {
                    t.balance += amount;
                }
            }
        }
        return map.values().toArray(new Total[map.size()]);
    }

    private Total getOrCreate(HashMap<Long, Total> map, Currency currency) {
        Total t = map.get(currency.id);
        if (t == null) {
            t = new Total(currency, true);
            map.put(currency.id, t);
        }
        return t;
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

    protected void cleanupFilter(WhereFilter filter) {
        // fixing a bug with saving incorrect filter fot this report
        // have to remove it here
        filter.remove("left");
        filter.remove("right");
    }

}
