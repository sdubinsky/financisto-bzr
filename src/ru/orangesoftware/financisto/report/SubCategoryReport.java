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
import ru.orangesoftware.financisto.activity.BlotterActivity;
import ru.orangesoftware.financisto.activity.SplitsBlotterActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.graph.GraphStyle;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.CategoryTree.NodeCreator;
import ru.orangesoftware.financisto.utils.CurrencyCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.orangesoftware.financisto.db.DatabaseHelper.V_REPORT_SUB_CATEGORY;

public class SubCategoryReport extends AbstractReport {
	
	public SubCategoryReport(Context context) {
		super(context);		
	}

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
		filterTransfers(filter);
		Cursor c = db.db().query(V_REPORT_SUB_CATEGORY, DatabaseHelper.SubCategoryReportColumns.NORMAL_PROJECTION,
				filter.getSelection(), filter.getSelectionArgs(), null, null,
                DatabaseHelper.SubCategoryReportColumns.CURRENCY_ID+","+DatabaseHelper.SubCategoryReportColumns.LEFT);
        try {
            CategoryTree<CategoryAmount> amounts = CategoryTree.createFromCursor(c, new NodeCreator<CategoryAmount>(){
                @Override
                public CategoryAmount createNode(Cursor c) {
                    return new CategoryAmount(c);
                }
            });

            ArrayList<GraphUnitTree> roots = createTree(amounts, 0);
            ArrayList<GraphUnit> units = new ArrayList<GraphUnit>();
            flattenTree(roots, units);
            Total[] totals = calculateTotals(roots);
            return new ReportData(units, totals);
        } finally {
            c.close();
        }
	}
	
	private ArrayList<GraphUnitTree> createTree(CategoryTree<CategoryAmount> amounts, int level) {
		ArrayList<GraphUnitTree> roots = new ArrayList<GraphUnitTree>();
		GraphUnitTree u = null;
		long lastId = -1;
		for (CategoryAmount a : amounts) {
			if (u == null || lastId != a.id) {
				u = new GraphUnitTree(a.id, a.title, getStyle(level));
				roots.add(u);
				lastId = a.id;
			}
            Currency c = CurrencyCache.getCurrency(a.currencyId);
			u.addAmount(c, a.amount);
			if (a.hasChildren()) {
				u.setChildren(createTree(a.children, level+1));
				u = null;				
			}
		}
        for (GraphUnitTree root : roots) {
            root.flatten();
        }
		Collections.sort(roots);
		return roots;
	}

	private void flattenTree(List<GraphUnitTree> tree, List<GraphUnit> units) {
		for (GraphUnitTree t : tree) {
			units.add(t);
			if (t.hasChildren()) {
				flattenTree(t.children, units);
				t.setChildren(null);
			}
		}
	}
	
	private static final GraphStyle[] STYLES = new GraphStyle[3];
	
	static {
		STYLES[0] = new GraphStyle.Builder().dy(2).textDy(5).lineHeight(30).nameTextSize(14).amountTextSize(12).indent(0).build();
		STYLES[1] = new GraphStyle.Builder().dy(2).textDy(5).lineHeight(20).nameTextSize(12).amountTextSize(10).indent(10).build();
		STYLES[2] = new GraphStyle.Builder().dy(2).textDy(5).lineHeight(20).nameTextSize(12).amountTextSize(10).indent(30).build();
	}

	private GraphStyle getStyle(int level) {
		return STYLES[Math.min(2, level)];
	}

	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		Category c = db.getCategory(id);
		return Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(c.left), String.valueOf(c.right));
	}

    @Override
    protected Class<? extends BlotterActivity> getBlotterActivityClass() {
        return SplitsBlotterActivity.class;
    }

	private static class CategoryAmount extends CategoryEntity<CategoryAmount> {
		
		private final long currencyId;
		private final long amount;	

		public CategoryAmount(Cursor c) {
			id = c.getLong(0);
			title = c.getString(1);
			currencyId = c.getLong(2);
			amount = c.getLong(3);	
			left = c.getInt(4);	
			right = c.getInt(5);	
		}

	}
	
	private static class GraphUnitTree extends GraphUnit {

		public List<GraphUnitTree> children;
		
		public GraphUnitTree(long id, String name, GraphStyle style) {
			super(id, name, style);
		}
		
		public void setChildren(List<GraphUnitTree> children) {
			this.children = children;
		}
		
		public boolean hasChildren() {
			return children != null && children.size() > 0;
		}

        @Override
        public void flatten() {
            super.flatten();
            if (children != null) {
                for (GraphUnitTree child : children) {
                    child.flatten();
                }
                Collections.sort(children);
            }
        }
    }

}

