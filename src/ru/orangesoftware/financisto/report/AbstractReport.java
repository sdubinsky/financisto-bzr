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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import ru.orangesoftware.financisto.activity.BlotterActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.graph.Amount;
import ru.orangesoftware.financisto.graph.GraphStyle;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public abstract class AbstractReport implements Report {
	
	public final GraphStyle style;
	
	protected final Context context;
    protected final boolean skipTransfers;
	
	public AbstractReport(Context context) {
		this.context = context;
		this.skipTransfers = !MyPreferences.isIncludeTransfersIntoReports(context);
        this.style = new GraphStyle.Builder(context).build();
	}
	
	protected String alterName(long id, String name) {
		return name;
	}
	
	protected ReportData queryReport(DatabaseAdapter db, String table, WhereFilter filter) {
		filterTransfers(filter);
		Cursor c = db.db().query(table, DatabaseHelper.ReportColumns.NORMAL_PROJECTION,
                filter.getSelection(), filter.getSelectionArgs(), null, null, "_id");
		ArrayList<GraphUnit> units = getUnitsFromCursor(db.em(), c);
        Total[] totals = calculateTotals(units);
        return new ReportData(units, totals);
	}

    protected void filterTransfers(WhereFilter filter) {
		if (skipTransfers) {
			filter.put(Criteria.eq(ReportColumns.IS_TRANSFER, "0"));
		}
	}

	protected ArrayList<GraphUnit> getUnitsFromCursor(MyEntityManager em, Cursor c) {
		try {
			ArrayList<GraphUnit> units = new ArrayList<GraphUnit>();
			GraphUnit u = null;
			long lastId = -1;
			while (c.moveToNext()) {
				long id = getId(c);
				String name = c.getString(ReportColumns.Indicies.NAME);
				long currencyId = c.getLong(ReportColumns.Indicies.CURRENCY_ID);
				long amount = c.getLong(ReportColumns.Indicies.AMOUNT);
                long isTransfer = c.getLong(ReportColumns.Indicies.IS_TRANSFER);
                if (id != lastId) {
					if (u != null) {
						units.add(u);
					}
					u = new GraphUnit(id, alterName(id, name), style);
					lastId = id;
				}
				Currency currency = CurrencyCache.getCurrency(em, currencyId);
				u.addAmount(currency, amount, skipTransfers && isTransfer != 0);
			}
			if (u != null) {
				units.add(u);
			}
            for (GraphUnit unit : units) {
                unit.flatten();
            }
            Collections.sort(units);
			return units;
		} finally {
			c.close();
		}
	}
	
    protected Total[] calculateTotals(ArrayList<? extends GraphUnit> units) {
        HashMap<Long, Total> map = new HashMap<Long, Total>();
        for (GraphUnit u : units) {
            for (Amount a : u) {
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
		Intent intent = new Intent(context, getBlotterActivityClass());
		filter.toIntent(intent);
		return intent;
	}

    protected Class<? extends BlotterActivity> getBlotterActivityClass() {
        return BlotterActivity.class;
    }

    protected void cleanupFilter(WhereFilter filter) {
        // fixing a bug with saving incorrect filter fot this report
        // have to remove it here
        filter.remove("left");
        filter.remove("right");
    }

    @Override
    public boolean shouldDisplayTotal() {
        return true;
    }
}
