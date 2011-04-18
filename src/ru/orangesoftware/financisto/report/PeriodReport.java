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
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.DatabaseHelper.ReportColumns;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Total;
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
		for (int i=0; i<periods.length; i++) {
			Period p = periods[i];
			newFilter.put(Criteria.btw(ReportColumns.DATETIME, String.valueOf(p.start), String.valueOf(p.end)));
			Cursor c = db.db().query(V_REPORT_PERIOD, DatabaseHelper.ReportColumns.NORMAL_PROJECTION, 
					newFilter.getSelection(), newFilter.getSelectionArgs(), null, null, null);
			GraphUnit u = getUnitFromCursor(c, i);
			if (u.amounts.size() > 0) {
				units.add(u);
			}
		}
        Total[] totals = calculateTotals(units);
		return new ReportData(units, totals);
	}

	@Override
	protected String alterName(long id, String name) {
		Period p = periods[(int)id];
		return context.getString(p.type.titleId);
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
