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

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_REPORT_CATEGORY;

import java.util.ArrayList;

import ru.orangesoftware.financisto.activity.ReportActivity;
import ru.orangesoftware.financisto.activity.ReportsListActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Category;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class CategoryReport2 extends AbstractReport {
	
	private static final String PARENT_CATEGORY_ID_EXTRA = "parentCategoryId";

	private final long parentCategoryId;
	
	public CategoryReport2(Context context, Bundle extra) {
		super(context);		
		parentCategoryId = extra.getLong(PARENT_CATEGORY_ID_EXTRA, 0);
	}

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
		filter.eq("parent_id", String.valueOf(parentCategoryId));
		return queryReport(db, V_REPORT_CATEGORY, filter);
	}
	
	@Override
	public Intent createActivityIntent(Context context, DatabaseAdapter db, WhereFilter parentFilter, long id) {
		WhereFilter filter = WhereFilter.empty();
		Criteria c = parentFilter.get(BlotterFilter.DATETIME);
		if (c != null) {
			filter.put(c);
		}
		filterTransfers(filter);
		Category category = db.getCategory(id);
		filter.put(Criteria.gte("left", String.valueOf(category.left)));
		filter.put(Criteria.lte("right", String.valueOf(category.right)));
		Intent intent = new Intent(context, ReportActivity.class);
		filter.toIntent(intent);
		intent.putExtra(ReportsListActivity.EXTRA_REPORT_TYPE, ReportType.BY_SUB_CATEGORY.name());
		return intent;
	}

	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		Category c = db.getCategory(id);
		return Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(c.left), String.valueOf(c.right));
	}
}

