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

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_REPORT_PROJECTS;

import java.util.ArrayList;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.graph.GraphUnit;
import android.content.Context;
import android.database.Cursor;

public class ProjectsReport extends AbstractReport {

	public ProjectsReport(Context context) {
		super(context);		
	}

	@Override
	public ArrayList<GraphUnit> getReport(DatabaseAdapter db, WhereFilter filter) {
		Cursor c = db.db().query(V_REPORT_PROJECTS, DatabaseHelper.ReportColumns.NORMAL_PROJECTION,  
				filter.getSelection(), filter.getSelectionArgs(), null, null, "_id");
		return getUnitsFromCursorAndSort(c);
	}

	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(id));
	}		
	
}
