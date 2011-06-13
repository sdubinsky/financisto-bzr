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
package ru.orangesoftware.financisto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.Criteria;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.utils.DateUtils.Period;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.MyPreferences.AccountSortOrder;
import ru.orangesoftware.financisto.utils.MyPreferences.LocationsSortOrder;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;
import ru.orangesoftware.orb.EntityManager;
import ru.orangesoftware.orb.Expression;
import ru.orangesoftware.orb.Expressions;
import ru.orangesoftware.orb.Query;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;

public class MyEntityManager extends EntityManager {
	
	private final Context context;
	
	public MyEntityManager(Context context, SQLiteDatabase db) {
		super(db);		
		this.context = context;
	}
	
	private <T extends MyEntity> ArrayList<T> getAllEntitiesList(Class<T> clazz, boolean include0) {
		Query<T> q = createQuery(clazz);
		if (!include0) {
			q.where(Expressions.neq("id", 0));
		}
		q.asc("title");
		Cursor c = q.execute();
		try {
			T e0 = null;
			ArrayList<T> list = new ArrayList<T>();
			while (c.moveToNext()) {
				T e = EntityManager.loadFromCursor(c, clazz);
				if (e.id == 0) {
					e0 = e;
				} else {
					list.add(e);
				}
			}
			if (e0 != null) {
				list.add(0, e0);
			}
			return list;
		} finally {
			c.close();
		}
	}

	/* ===============================================
	 * LOCATION
	 * =============================================== */
	
	public Cursor getAllLocations(boolean includeCurrentLocation) {
		Query<MyLocation> q = createQuery(MyLocation.class); 
		if (!includeCurrentLocation) {
			q.where(Expressions.neq("id", 0));
		}
		LocationsSortOrder sortOrder = MyPreferences.getLocationsSortOrder(context);		
		if (sortOrder.asc) {
			q.asc(sortOrder.property);
		} else {
			q.desc(sortOrder.property);
		}		
		if (sortOrder != LocationsSortOrder.NAME) {
			q.asc(LocationsSortOrder.NAME.property);
		}
		Cursor c = q.execute();
		//DatabaseUtils.dumpCursor(c);
		return c;
	}

	public ArrayList<MyLocation> getAllLocationsList(boolean includeNoLocation) {
		Cursor c = getAllLocations(includeNoLocation);
		try {
			MyLocation e0 = null;
			ArrayList<MyLocation> list = new ArrayList<MyLocation>();
			while (c.moveToNext()) {
				MyLocation e = EntityManager.loadFromCursor(c, MyLocation.class);
				if (e.id == 0) {
					e0 = e;
				} else {
					list.add(e);
				}
			}
			if (e0 != null) {
				list.add(0, e0);
			}
			return list;
		} finally {
			c.close();
		}
	}

	public void deleteLocation(long id) {
		db.beginTransaction();
		try {
			delete(MyLocation.class, id);
			ContentValues values = new ContentValues();
			values.put("location_id", 0);
			db.update("transactions", values, "location_id=?", new String[]{String.valueOf(id)});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}		
	}

	public long saveLocation(MyLocation location) {
		return saveOrUpdate(location);
	}

	/* ===============================================
	 * TRANSACTION INFO
	 * =============================================== */

	public TransactionInfo getTransactionInfo(long transactionId) {
		return get(TransactionInfo.class, transactionId);
	}

	public TransactionInfo loadTransactionInfo(long transactionId) {
		return load(TransactionInfo.class, transactionId);
	}

	public List<TransactionAttributeInfo> getAttributesForTransaction(long transactionId) {
		Query<TransactionAttributeInfo> q = createQuery(TransactionAttributeInfo.class).asc("name");
		q.where(Expressions.and(
					Expressions.eq("transactionId", transactionId),
					Expressions.gte("attributeId", 0)
				));
		Cursor c = q.execute();
		try {
			List<TransactionAttributeInfo> list = new LinkedList<TransactionAttributeInfo>();
			while (c.moveToNext()) {
				TransactionAttributeInfo ti = loadFromCursor(c, TransactionAttributeInfo.class);
				list.add(ti);
			}
			return list;
		} finally {
			c.close();
		}
	
	}

	public TransactionAttributeInfo getSystemAttributeForTransaction(SystemAttribute sa, long transactionId) {
		Query<TransactionAttributeInfo> q = createQuery(TransactionAttributeInfo.class); 
		q.where(Expressions.and(
				Expressions.eq("transactionId", transactionId),
				Expressions.eq("attributeId", sa.id)
		));
		Cursor c = q.execute();
		try {
			if (c.moveToFirst()) {
				return loadFromCursor(c, TransactionAttributeInfo.class);
			}
			return null;
		} finally {
			c.close();
		}
	}

	/* ===============================================
	 * ACCOUNT
	 * =============================================== */
	
	public Account getAccount(long id) {
		return get(Account.class, id);
	}
	
	public Cursor getAccountsForTransaction(Transaction t) {
		return getAllAccounts(true, t.fromAccountId, t.toAccountId);
	}

	public Cursor getAllActiveAccounts() {
		return getAllAccounts(true);
	}
	
	public Cursor getAllAccounts() {
		return getAllAccounts(false);	
	}

	private Cursor getAllAccounts(boolean isActiveOnly, long...includeAccounts) {
		AccountSortOrder sortOrder = MyPreferences.getAccountSortOrder(context);
		Query<Account> q = createQuery(Account.class);
		if (isActiveOnly) {
			int count = includeAccounts.length;
			if (count > 0) {
				Expression[] ee = new Expression[count+1];				
				for (int i=0; i<count; i++) {
					ee[i] = Expressions.eq("id", includeAccounts[i]);
				}
				ee[count] = Expressions.eq("isActive", 1);
				q.where(Expressions.or(ee));
			} else {
				q.where(Expressions.eq("isActive", 1));				
			}			
		}
		q.desc("isActive");
		if (sortOrder.asc) {
			q.asc(sortOrder.property);
		} else {
			q.desc(sortOrder.property);
		}		
		return q.asc("title").execute();
	}

	public long saveAccount(Account account) {
		return saveOrUpdate(account);
	}

	public ArrayList<Account> getAllAccountsList() {
		ArrayList<Account> list = new ArrayList<Account>();
		Cursor c = getAllAccounts();
		try {
			while (c.moveToNext()) {
				Account a = EntityManager.loadFromCursor(c, Account.class);
				list.add(a);
			}			
		} finally {
			c.close();
		}
		return list;
	}

	/* ===============================================
	 * CURRENCY
	 * =============================================== */

	private static final String UPDATE_DEFAULT_FLAG = "update currency set is_default=0";
	
	public long saveOrUpdate(Currency currency) {
		db.beginTransaction();
		try {
			if (currency.isDefault) {
				db.execSQL(UPDATE_DEFAULT_FLAG);
			}
			long id = super.saveOrUpdate(currency);
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
	}

	public int deleteCurrency(long id) {
		String sid = String.valueOf(id);
		return db.delete(CURRENCY_TABLE, "_id=? AND NOT EXISTS (SELECT 1 FROM "+ACCOUNT_TABLE+" WHERE "+AccountColumns.CURRENCY_ID+"=?)", 
				new String[]{sid, sid});
	}
	
	public Cursor getAllCurrencies(String sortBy) {
		Query<Currency> q = createQuery(Currency.class);
		return q.desc("isDefault").asc(sortBy).execute();
	}

	/* ===============================================
	 * TRANSACTIONS
	 * =============================================== */

//	public Cursor getBlotter(WhereFilter blotterFilter) {
//		long t0 = System.currentTimeMillis();
//		try {
//			Query<TransactionInfo> q = createQuery(TransactionInfo.class);
//			if (!blotterFilter.isEmpty()) {
//				q.where(blotterFilter.toWhereExpression());
//			}
//			q.desc("dateTime");
//			return q.list();
//		} finally {
//			Log.d("BLOTTER", "getBlotter executed in "+(System.currentTimeMillis()-t0)+"ms");
//		}
//	}
//
//	public Cursor getTransactions(WhereFilter blotterFilter) {
//		return null;
//	}

//	public Cursor getAllProjects(boolean includeNoProject) {
//		Query<Project> q = createQuery(Project.class);
//		if (!includeNoProject) {
//			q.where(Expressions.neq("id", 0));
//		}
//		return q.list();
//	}

	public Project getProject(long id) {
		return get(Project.class, id);
	}

	public ArrayList<Project> getAllProjectsList(boolean includeNoProject) {
		return getAllEntitiesList(Project.class, includeNoProject);
	}

//	public Category getCategoryByLeft(long left) {
//		Query<Category> q = createQuery(Category.class);
//		q.where(Expressions.eq("left", left));
//		return q.uniqueResult();
//	}
//
//	public Cursor getAllCategories(boolean includeNoCategory) {
//		Query<CategoryInfo> q = createQuery(CategoryInfo.class);
//		if (!includeNoCategory) {
//			q.where(Expressions.neq("id", 0));
//		}
//		return q.list();
//	}
//	
//	public Cursor getAllCategoriesWithoutSubtree(long id) {
//		Category c = load(Category.class, id);
//		Query<CategoryInfo> q = createQuery(CategoryInfo.class);
//		q.where(Expressions.not(Expressions.and(
//				Expressions.gte("left", c.left),
//				Expressions.lte("right", c.right)
//		)));
//		return q.list();
//	}

	public long insertBudget(Budget budget) {
		db.beginTransaction();
		try {
			if (budget.id > 0) {
				deleteBudget(budget.id);
			}
			long id = 0;
			Recur recur = RecurUtils.createFromExtraString(budget.recur);
			Period[] periods = RecurUtils.periods(recur);			
			for (int i=0; i<periods.length; i++) {
				Period p = periods[i];
				budget.id = -1;
				budget.parentBudgetId = id;
				budget.recurNum = i;
				budget.startDate = p.start;
				budget.endDate = p.end;								
				long bid = super.saveOrUpdate(budget);
				if (i == 0) {
					id = bid;
				}
			}
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
	}

	public void deleteBudget(long id) {
		db.delete(BUDGET_TABLE, "_id=?", new String[]{String.valueOf(id)});
		db.delete(BUDGET_TABLE, "parent_budget_id=?", new String[]{String.valueOf(id)});
	}

	public void deleteBudgetOneEntry(long id) {
		db.delete(BUDGET_TABLE, "_id=?", new String[]{String.valueOf(id)});
	}

	public ArrayList<Budget> getAllBudgets(WhereFilter filter) {
		Query<Budget> q = createQuery(Budget.class);
		Criteria c = filter.get(BlotterFilter.DATETIME);
		if (c != null) {
			long start = c.getLongValue1();
			long end = c.getLongValue2();
			q.where(Expressions.and(Expressions.lte("startDate", end), Expressions.gte("endDate", start)));
		}
		Cursor cursor = q.execute();
		try {
			ArrayList<Budget> list = new ArrayList<Budget>();
			while (cursor.moveToNext()) {
				Budget b = MyEntityManager.loadFromCursor(cursor, Budget.class);
				list.add(b);				
			}
			return list;
		} finally {
			cursor.close();
		}
	}

	public void deleteProject(long id) {
		db.beginTransaction();
		try {
			delete(Project.class, id);
			ContentValues values = new ContentValues();
			values.put("project_id", 0);
			db.update("transactions", values, "project_id=?", new String[]{String.valueOf(id)});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}		
	}

	public ArrayList<TransactionInfo> getAllScheduledTransactions() {
		Query<TransactionInfo> q = createQuery(TransactionInfo.class);
		q.where(Expressions.eq("isTemplate", 2));
		return (ArrayList<TransactionInfo>)q.list();
	}

	public Category getCategory(long id) {
		return get(Category.class, id);
	}

	public ArrayList<Category> getAllCategoriesList(boolean includeNoCategory) {
		return getAllEntitiesList(Category.class, includeNoCategory);
	}

    public Payee insertPayee(String payee) {
        Query<Payee> q = createQuery(Payee.class);
        q.where(Expressions.eq("title", payee));
        Payee p = q.uniqueResult();
        if (p == null) {
            p = new Payee();
            p.title = payee;
            p.id = saveOrUpdate(p);
        }
        return p;
    }

    public Cursor getAllPayees() {
        Query<Payee> q = createQuery(Payee.class);
        return q.asc("title").execute();
    }

    public ArrayList<Payee> getAllPayeeList() {
        return getAllEntitiesList(Payee.class, true);
    }

    public Cursor getAllPayeesLike(CharSequence constraint) {
        Query<Payee> q = createQuery(Payee.class);
        q.where(Expressions.like("title", "%"+constraint+"%"));
        return q.asc("title").execute();
    }

    public List<Split> getSplitsForTransaction(long transactionId) {
        Query<Split> q = createQuery(Split.class);
        q.where(Expressions.eq("transactionId", transactionId));
        return q.list();
    }

}
