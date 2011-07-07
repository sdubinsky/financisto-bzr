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
import android.database.Cursor;
import android.database.DatabaseUtils;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.DateUtils.*;

import java.util.ArrayList;

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_REPORT_PERIOD;
import static ru.orangesoftware.financisto.utils.DateUtils.*;

public class PeriodReport extends AbstractReport {
	
	private final Period[] periods = new Period[]{
			today(), yesterday(), thisWeek(), lastWeek(), thisMonth(), lastMonth()
	};	
	
	public PeriodReport(Context context) {
		super(context);		
	}

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
		WhereFilter newFilter = WhereFilter.empty();
		Criteria criteria = filter.get(ReportColumns.CURRENCY_ID);
		if (criteria != null) {
			newFilter.put(criteria);
		}
		filterTransfers(newFilter);
		ArrayList<GraphUnit> units = new ArrayList<GraphUnit>();
        for (Period p : periods) {
            newFilter.put(Criteria.btw(ReportColumns.DATETIME, String.valueOf(p.start), String.valueOf(p.end)));
            Cursor c = db.db().query(V_REPORT_PERIOD, ReportColumns.NORMAL_PROJECTION,
                    newFilter.getSelection(), newFilter.getSelectionArgs(), null, null, null);
            GraphUnit u = getUnitFromCursor(c, p);
            if (u.size() > 0) {
                units.add(u);
            }
        }
        Total[] totals = calculateTotals(units);
		return new ReportData(units, totals);
	}

    private GraphUnit getUnitFromCursor(Cursor c, Period p) {
        try {
            GraphUnit u = createGraphUnit(p);
            while (c.moveToNext()) {
                long currencyId = c.getLong(2);
                long amount = c.getLong(3);
                Currency currency = CurrencyCache.getCurrencyOrEmpty(currencyId);
                u.addAmount(currency, amount);
            }
            u.flatten();
            return u;
        } finally {
            c.close();
        }
    }

    private GraphUnit createGraphUnit(Period p) {
        return new GraphUnit(p.type.ordinal(), context.getString(p.type.titleId), DEFAULT_STYLE);
    }

    @Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		Period p = periods[(int)id];
		return new DateTimeCriteria(p);
	}

    @Override
    public boolean shouldDisplayTotal() {
        return false;
    }

}
