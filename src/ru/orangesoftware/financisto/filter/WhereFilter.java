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
package ru.orangesoftware.financisto.filter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.activity.DateFilterActivity;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.orb.Expression;
import ru.orangesoftware.orb.Expressions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

public class WhereFilter {
	
	public static final String TITLE_EXTRA = "title";
    public static final String FILTER_EXTRA = "filter";
    public static final String SORT_ORDER_EXTRA = "sort_order";

    public static final String FILTER_TITLE_PREF = "filterTitle";
    public static final String FILTER_LENGTH_PREF = "filterLength";
    public static final String FILTER_CRITERIA_PREF = "filterCriteria";
    public static final String FILTER_SORT_ORDER_PREF = "filterSortOrder";

	private final String title;
	private final LinkedList<Criteria> criterias = new LinkedList<Criteria>();
	private final LinkedList<String> sorts = new LinkedList<String>();

	public WhereFilter(String title) {
		this.title = title;
	}
	
	public WhereFilter eq(Criteria c) {
		criterias.add(c);
		return this;
	}

	public WhereFilter eq(String column, String value) {
		criterias.add(Criteria.eq(column, value));
		return this;
	}

    public WhereFilter neq(String column, String value) {
        criterias.add(Criteria.neq(column, value));
        return this;
    }

	public WhereFilter btw(String column, String value1, String value2) {
		criterias.add(Criteria.btw(column, value1, value2));
		return this;
	}
	
	public WhereFilter gt(String column, String value) {
		criterias.add(Criteria.gt(column, value));
		return this;
	}

	public WhereFilter gte(String column, String value) {
		criterias.add(Criteria.gte(column, value));
		return this;
	}
	
	public WhereFilter lt(String column, String value) {
		criterias.add(Criteria.lt(column, value));
		return this;
	}

	public WhereFilter lte(String column, String value) {
		criterias.add(Criteria.lte(column, value));
		return this;
	}

    public WhereFilter isNull(String column) {
        criterias.add(Criteria.isNull(column));
        return this;
    }

    public WhereFilter asc(String column) {
		sorts.add(column+" asc");
		return this;
	}

	public WhereFilter desc(String column) {
		sorts.add(column+" desc");
		return this;
	}

	private String getSelection(List<Criteria> criterias) {
		StringBuilder sb = new StringBuilder();
		for (Criteria c : criterias) {
			if (sb.length() > 0) {
				sb.append(" AND ");				
			}
			sb.append(c.getSelection());
		}
		return sb.toString().trim();
	}

	private String[] getSelectionArgs(List<Criteria> criterias) {
		String[] args = new String[0];
		for (Criteria c : criterias) {
			args = Utils.joinArrays(args, c.getSelectionArgs());
		}
		return args;
	}

	public Criteria get(String name) {
		for (Criteria c : criterias) {
			String column = c.columnName;
			if (name.equals(column)) {
				return c;
			}
		}
		return null;
	}	
	
	public DateTimeCriteria getDateTime() {
		return (DateTimeCriteria)get(BlotterFilter.DATETIME);
	}
	
	public Criteria put(Criteria criteria) {
		for (int i=0; i<criterias.size(); i++) {
			Criteria c = criterias.get(i);
			if (criteria.columnName.equals(c.columnName)) {
				criterias.set(i, criteria);
				return c;
			}
		}
		criterias.add(criteria);
		return null;
	}
	
	public Criteria remove(String name) {
		for (Iterator<Criteria> i = criterias.iterator(); i.hasNext();) {
			Criteria c = i.next();
			if (name.equals(c.columnName)) {
				i.remove();
				return c;
			}
		}
		return null;
	}
	
	public void clear() {
		criterias.clear();
		sorts.clear(); 
	}
	
	public static WhereFilter copyOf(WhereFilter filter) {
		WhereFilter f = new WhereFilter(filter.title);
		f.criterias.addAll(filter.criterias);
		f.sorts.addAll(filter.sorts);
		return f;
	}
	
	public static WhereFilter empty() {
		return new WhereFilter("");
	}
	
	public Expression toWhereExpression() {
		int count = criterias.size();
		Expression[] ee = new Expression[count];
		for (int i=0; i<count; i++) {
			ee[i] = criterias.get(i).toWhereExpression();
		}		
		return Expressions.and(ee);
	}

	public void toBundle(Bundle bundle) {		
		String[] extras = new String[criterias.size()];
		for (int i=0; i<extras.length; i++) {
			extras[i] = criterias.get(i).toStringExtra();
		}
		bundle.putString(TITLE_EXTRA, title);
		bundle.putStringArray(FILTER_EXTRA, extras);
		bundle.putString(SORT_ORDER_EXTRA, getSortOrder());
	}

	public static WhereFilter fromBundle(Bundle bundle) {
		String title = bundle.getString(TITLE_EXTRA);
		WhereFilter filter = new WhereFilter(title);
		String[] a = bundle.getStringArray(FILTER_EXTRA);
		if (a != null) {
            for (String s : a) {
                filter.put(Criteria.fromStringExtra(s));
            }
		}
		String sortOrder = bundle.getString(SORT_ORDER_EXTRA);
		if (sortOrder != null) {
			String[] orders = sortOrder.split(",");
			if (orders != null && orders.length > 0) {
				filter.sorts.addAll(Arrays.asList(orders));
			}
		}
		return filter;
	}

	public void toIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle == null) bundle = new Bundle();		
		toBundle(bundle);
		intent.replaceExtras(bundle);
	}

	public static WhereFilter fromIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle == null) bundle = new Bundle();
		return fromBundle(bundle);
	}
	
	public String getSortOrder() {
		StringBuilder sb = new StringBuilder();
		for (String o : sorts) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(o);
		}
		return sb.toString();
	}

	public void resetSort() {
		sorts.clear();
	}
	
	public void toSharedPreferences(SharedPreferences preferences) {
		Editor e = preferences.edit();
		int count = criterias.size();
		e.putString(FILTER_TITLE_PREF, title);
		e.putInt(FILTER_LENGTH_PREF, count);
		for (int i=0; i<count; i++) {
			e.putString(FILTER_CRITERIA_PREF+i, criterias.get(i).toStringExtra());
		}
		e.putString(FILTER_SORT_ORDER_PREF, getSortOrder());
		e.commit();
	}
	
	public static WhereFilter fromSharedPreferences(SharedPreferences preferences) {
		String title = preferences.getString(FILTER_TITLE_PREF, "");
		WhereFilter filter = new WhereFilter(title);
		int count = preferences.getInt(FILTER_LENGTH_PREF, 0);
		if (count > 0) {
			for (int i=0; i<count; i++) {
				String criteria = preferences.getString(FILTER_CRITERIA_PREF+i, "");
				if (criteria.length() > 0) {
					filter.put(Criteria.fromStringExtra(criteria));
				}
			}
		}
		String sortOrder = preferences.getString(FILTER_SORT_ORDER_PREF, "");
		String[] orders = sortOrder.split(",");
		if (orders != null && orders.length > 0) {
			filter.sorts.addAll(Arrays.asList(orders));
		}
		return filter;		
	}
	
	public String getSelection() {
		return getSelection(criterias);
	}
	
	public String[] getSelectionArgs() {
		return getSelectionArgs(criterias);
	}		
	
	public long getAccountId() {
		Criteria c = get(BlotterFilter.FROM_ACCOUNT_ID);
		return c != null ? c.getLongValue1() : -1;
	}
	
	public long getBudgetId() {
		Criteria c = get(BlotterFilter.BUDGET_ID);
		return c != null ? c.getLongValue1() : -1;
	}

	public int getIsTemplate() {
		Criteria c = get(BlotterFilter.IS_TEMPLATE);
		return c != null ? c.getIntValue() : 0;
	}

	public boolean isTemplate() {
		Criteria c = get(BlotterFilter.IS_TEMPLATE);
		return c != null && c.getLongValue1() == 1;
	}
	
	public boolean isSchedule() {
		Criteria c = get(BlotterFilter.IS_TEMPLATE);
		return c != null && c.getLongValue1() == 2;
	}

	public String getTitle() {
		return title;
	}
	
	public boolean isEmpty() {
		return criterias.isEmpty();
	}
	
	public static enum Operation {
		NOPE(""), EQ("=?"), NEQ("!=?"), GT(">?"), GTE(">=?"), LT("<?"), LTE("<=?"), BTW("BETWEEN ? AND ?"), ISNULL("is NULL");
		
		public final String op;
		
		private Operation(String op) {
			this.op = op;
		}		
	}

    public void clearDateTime() {
		remove(BlotterFilter.DATETIME);
	}
	
	public static DateTimeCriteria dateTimeFromIntent(Intent data) {
		String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
		PeriodType p = PeriodType.valueOf(periodType);
		if (PeriodType.CUSTOM == p) {
			long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
			long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
			return new DateTimeCriteria(periodFrom, periodTo);
		} else {
			return new DateTimeCriteria(p);
		}
		
	}

}
