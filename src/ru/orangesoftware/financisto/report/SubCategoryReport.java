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

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_REPORT_SUB_CATEGORY;

import java.util.ArrayList;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Category;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

public class SubCategoryReport extends AbstractReport {
	
	public SubCategoryReport(Context context, Bundle extra) {
		super(context);		
	}

	@Override
	public ArrayList<GraphUnit> getReport(DatabaseAdapter db, WhereFilter filter) {
		Cursor c = db.db().query(V_REPORT_SUB_CATEGORY, DatabaseHelper.ReportColumns.NORMAL_PROJECTION,
				filter.getSelection(), filter.getSelectionArgs(), null, null, "left");
		//DatabaseUtils.dumpCursor(c);
		return getUnitsFromCursor(c);
	}
	
	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		Category c = db.getCategory(id);
		return Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(c.left), String.valueOf(c.right));
	}
}

