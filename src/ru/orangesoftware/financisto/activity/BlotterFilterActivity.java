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
package ru.orangesoftware.financisto.activity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.R.array;
import ru.orangesoftware.financisto.R.id;
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryViewColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.DateUtils.Period;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;


public class BlotterFilterActivity extends AbstractActivity {	
	
	private WhereFilter filter = WhereFilter.empty();
	
	private TextView period;
	private TextView account;
	private TextView currency;
	private TextView category;
	private TextView project;
	private TextView location;
	private TextView sortOrder;
	
	private DateFormat df;
	private String[] sortBlotterEntries;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.blotter_filter);
		
		df = DateUtils.getShortDateFormat(this);
		sortBlotterEntries = getResources().getStringArray(R.array.sort_blotter_entries);
		
		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);
		period = x.addListNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
		account = x.addListNodeMinus(layout, R.id.account, R.id.account_clear, R.string.account, R.string.no_filter);
		currency = x.addListNodeMinus(layout, R.id.currency, R.id.currency_clear, R.string.currency, R.string.no_filter);
		category = x.addListNodeMinus(layout, R.id.category, R.id.category_clear, R.string.category, R.string.no_filter);
		project = x.addListNodeMinus(layout, R.id.project, R.id.project_clear, R.string.project, R.string.no_filter);
		location = x.addListNodeMinus(layout, R.id.location, R.id.location_clear, R.string.location, R.string.no_filter);
		sortOrder = x.addListNodeMinus(layout, R.id.sort_order, R.id.sort_order_clear, R.string.sort_order, sortBlotterEntries[0]);
		
		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent data = new Intent();
				filter.toIntent(data);
				setResult(RESULT_OK, data);
				finish();
			}
		});
		
		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		ImageButton bNoFilter = (ImageButton)findViewById(R.id.bNoFilter);
		bNoFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setResult(RESULT_FIRST_USER);
				finish();
			}
		});		
		
		Intent intent = getIntent();
		if (intent != null) {
			filter = WhereFilter.fromIntent(intent);
			updatePeriodFromFilter();
			updateAccountFromFilter();
			updateCurrencyFromFilter();
			updateCategoryFromFilter();
			updateProjectFromFilter();
			updateLocationFromFilter();
			updateSortOrderFromFilter();
		}
		
	}

	private void updateSortOrderFromFilter() {
		String s = filter.getSortOrder();
		if (BlotterFilter.SORT_OLDER_TO_NEWER.equals(s)) {
			sortOrder.setText(sortBlotterEntries[1]);
		} else {
			sortOrder.setText(sortBlotterEntries[0]);
		}
	}

	private void updateLocationFromFilter() {
		Criteria c = filter.get(BlotterFilter.LOCATION_ID);
		if (c != null) {
			MyLocation loc = em.getLocation(c.getLongValue1());
			location.setText(loc.name);
		} else {
			location.setText(R.string.no_filter);
		}
	}

	private void updateProjectFromFilter() {
		Criteria c = filter.get(BlotterFilter.PROJECT_ID);
		if (c != null) {
			Project p = em.load(Project.class, c.getLongValue1());
			project.setText(p.title);
		} else {
			project.setText(R.string.no_filter);
		}
	}

	private void updateCategoryFromFilter() {
		Criteria c = filter.get(BlotterFilter.CATEGORY_LEFT);
		if (c != null) {
			Category cat = db.getCategoryByLeft(c.getLongValue1());
			category.setText(cat.title);
		} else {
			category.setText(R.string.no_filter);
		}
	}

	private void updatePeriodFromFilter() {
		DateTimeCriteria c = (DateTimeCriteria)filter.get(BlotterFilter.DATETIME);
		if (c != null) {
			Period p = c.getPeriod();
			if (p.isCustom()) {
				long periodFrom = c.getLongValue1();
				long periodTo = c.getLongValue2();
				period.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
			} else {
				period.setText(p.type.titleId);
			}
		} else {
			period.setText(R.string.no_filter);
		}
	}

	private void updateAccountFromFilter() {
		Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_ID);
		if (c != null) {
			Account a = em.getAccount(c.getLongValue1());
			if (a != null) {
				account.setText(a.title);
			}
		} else {
			account.setText(R.string.no_filter);
		}
	}

	private void updateCurrencyFromFilter() {
		Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID);
		if (c != null) {
			Currency cur = em.load(Currency.class, c.getLongValue1());
			currency.setText(cur.name);
		} else {
			currency.setText(R.string.no_filter);
		}
	}

	@Override
	protected void onClick(View v, int id) {
		switch (id) {
		case R.id.period:
			Intent intent = new Intent(this, DateFilterActivity.class);
			filter.toIntent(intent);
			startActivityForResult(intent, 1);
			break;
		case R.id.period_clear:
			filter.remove(BlotterFilter.DATETIME);
			period.setText(R.string.no_filter);
			break;
		case R.id.account: {
			Cursor cursor = em.getAllAccounts();
			startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createAccountAdapter(this, cursor);
			Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(R.id.account, R.string.account, cursor, adapter, "_id", selectedId);
		} break;
		case R.id.account_clear:
			clear(BlotterFilter.FROM_ACCOUNT_ID, account);
			break;
		case R.id.currency: {
			Cursor cursor = em.getAllCurrencies("name");
			startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, cursor);
			Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(R.id.currency, R.string.currency, cursor, adapter, "_id", selectedId);
		} break;
		case R.id.currency_clear:
			clear(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, currency);
			break;
		case R.id.category: {
			Cursor cursor = db.getAllCategories(false);
			startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createCategoryAdapter(db, this, cursor);
			Criteria c = filter.get(BlotterFilter.CATEGORY_LEFT);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(R.id.category, R.string.category, cursor, adapter, CategoryViewColumns.LEFT, selectedId);
		} break;
		case R.id.category_clear:
			clear(BlotterFilter.CATEGORY_LEFT, category);
			break;
		case R.id.project: {
			ArrayList<Project> projects = em.getAllProjectsList(false);
//			Cursor cursor = em.getAllProjects(false);
//			startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createProjectAdapter(this, projects);
			Criteria c = filter.get(BlotterFilter.PROJECT_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			int selectedPos = MyEntity.indexOf(projects, selectedId);
			x.select(R.id.project, R.string.project, adapter, selectedPos);
		} break;
		case R.id.project_clear:
			clear(BlotterFilter.PROJECT_ID, project);
			break;
		case R.id.location: {
			Cursor cursor = em.getAllLocations(false);
			startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createLocationAdapter(this, cursor);
			Criteria c = filter.get(BlotterFilter.LOCATION_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(R.id.location, R.string.location, cursor, adapter, "_id", selectedId);
		} break;
		case R.id.location_clear:
			clear(BlotterFilter.LOCATION_ID, location);
			break;
		case R.id.sort_order: {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, sortBlotterEntries);
			int selectedId = BlotterFilter.SORT_OLDER_TO_NEWER.equals(filter.getSortOrder()) ? 1 : 0;
			x.select(R.id.sort_order, R.string.sort_order, adapter, selectedId);
		} break;
		case R.id.sort_order_clear:
			filter.resetSort();
			filter.desc(BlotterFilter.DATETIME);
			updateSortOrderFromFilter();
			break;
		}
	}

	private void clear(String criteria, TextView textView) {
		filter.remove(criteria);
		textView.setText(R.string.no_filter);
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		switch (id) {
		case R.id.account:
			filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(selectedId)));
			updateAccountFromFilter();
			break;
		case R.id.currency:
			filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, String.valueOf(selectedId)));
			updateCurrencyFromFilter();
			break;
		case R.id.category:
			Category cat = db.getCategoryByLeft(selectedId);
			filter.put(Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
			updateCategoryFromFilter();
			break;
		case R.id.project:
			filter.put(Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(selectedId)));
			updateProjectFromFilter();
			break;
		case R.id.location:
			filter.put(Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(selectedId)));
			updateLocationFromFilter();
			break;
		case R.id.sort_order:
			filter.resetSort();
			if (selectedId == 1) {
				filter.asc(BlotterFilter.DATETIME);
			} else {
				filter.desc(BlotterFilter.DATETIME);
			}
			updateSortOrderFromFilter();
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_FIRST_USER) {
				onClick(period, R.id.period_clear);
			} else if (resultCode == RESULT_OK) {
				DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
				filter.put(c);
				updatePeriodFromFilter();
			}
		}
	}
	
}
