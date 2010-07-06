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
import java.util.Collections;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.graph.GraphStyle;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryEntity;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.CategoryTree.NodeCreator;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

public class SubCategoryReport extends AbstractReport {
	
	public SubCategoryReport(Context context, Bundle extra) {
		super(context);		
	}

	@Override
	public ArrayList<GraphUnit> getReport(DatabaseAdapter db, WhereFilter filter) {
		Cursor c = db.db().query(V_REPORT_SUB_CATEGORY, DatabaseHelper.SubCategoryReportColumns.NORMAL_PROJECTION,
				filter.getSelection(), filter.getSelectionArgs(), null, null, "left");
		CategoryTree<CategoryAmount> amounts = CategoryTree.createFromCursor(c, new NodeCreator<CategoryAmount>(){
			@Override
			public CategoryAmount createNode(Cursor c) {
				return new CategoryAmount(c);
			}
		});

		ArrayList<GraphUnitTree> roots = createTree(amounts, 0);
		ArrayList<GraphUnit> units = new ArrayList<GraphUnit>();
		flatenTree(roots, units);
		return units;
	}
	
	private ArrayList<GraphUnitTree> createTree(CategoryTree<CategoryAmount> amounts, int level) {
		ArrayList<GraphUnitTree> roots = new ArrayList<GraphUnitTree>();
		GraphUnitTree u = null;
		Currency c = null;
		long lastId = -1;
		for (CategoryAmount a : amounts) {
			if (u == null || lastId != a.id) {
				u = new GraphUnitTree(a.id, a.title, getStyle(level));
				c = CurrencyCache.getCurrency(a.currencyId);
				roots.add(u);
				lastId = a.id;
			}
			u.addAmount(c, a.amount);
			if (a.hasChildren()) {
				u.setChildren(createTree(a.children, level+1));
				u = null;				
			}
		}
		Collections.sort(roots);
		return roots;
	}

	private void flatenTree(ArrayList<GraphUnitTree> tree, ArrayList<GraphUnit> units) {
		for (GraphUnitTree t : tree) {
			units.add(t);
			if (t.hasChildren()) {
				flatenTree(t.children, units);
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

		public ArrayList<GraphUnitTree> children;
		
		public GraphUnitTree(long id, String name, GraphStyle style) {
			super(id, name, style);
		}
		
		public void setChildren(ArrayList<GraphUnitTree> children) {
			this.children = children;
		}
		
		public boolean hasChildren() {
			return children != null && children.size() > 0;
		}
		
	}

}

