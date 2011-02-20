/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - implement getAllExpenses method for bill filtering
 ******************************************************************************/
package ru.orangesoftware.financisto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.CategoryTree.NodeCreator;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.IOException;
import java.util.*;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;

public class DatabaseAdapter {

	private final Context context;
	private final DatabaseHelper dbHelper;
	
	private SQLiteDatabase db;
	private MyEntityManager em;
	
	public DatabaseAdapter(Context context) {
		this.context = context;
		this.dbHelper = new DatabaseHelper(context);
	}
	
	public void forceRunAlterScript(String name) throws IOException {
		dbHelper.forceRunAlterScript(db, name);
	}
	
	public void open() throws SQLiteException {
		try {
			db = dbHelper.getWritableDatabase();
		} catch (SQLiteException ex) {
			db = dbHelper.getReadableDatabase();
		}
		em = new MyEntityManager(context, db);
	}

	public void close() {
		db.close();
		db = null;
		em = null;
	}
	
	public SQLiteDatabase db() {
		return db;
	}

	public MyEntityManager em() {
		return em;
	}

	// ===================================================================
	// ACCOUNT
	// ===================================================================

	private static final String UPDATE_ORPHAN_TRANSACTIONS_1 = "UPDATE "+TRANSACTION_TABLE+" SET "+
								TransactionColumns.to_account_id +"=0, "+
								TransactionColumns.to_amount +"=0 "+
								"WHERE "+TransactionColumns.to_account_id +"=?";
	private static final String UPDATE_ORPHAN_TRANSACTIONS_2 = "UPDATE "+TRANSACTION_TABLE+" SET "+
								TransactionColumns.from_account_id +"="+TransactionColumns.to_account_id +", "+
								TransactionColumns.from_amount +"="+TransactionColumns.to_amount +", "+
								TransactionColumns.to_account_id +"=0, "+
								TransactionColumns.to_amount +"=0 "+
								"WHERE "+TransactionColumns.from_account_id +"=? AND "+
										 TransactionColumns.to_account_id +">0";
	
	public int deleteAccount(long id) {
		db.beginTransaction();
		try {
			String[] sid = new String[]{String.valueOf(id)};
			db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_1, sid);
			db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_2, sid);
			db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID
					+" in (SELECT _id from "+TRANSACTION_TABLE+" where "+TransactionColumns.from_account_id +"=?)", sid);
			db.delete(TRANSACTION_TABLE, TransactionColumns.from_account_id +"=?", sid);
			int count = db.delete(ACCOUNT_TABLE, "_id=?", sid); 
			db.setTransactionSuccessful();
			return count;
		} finally {
			db.endTransaction();
		}
		
	}
	
	// ===================================================================
	// TRANSACTION
	// ===================================================================
	
	public Transaction getTransaction(long id) {
		Cursor c = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
				TransactionColumns._id +"=?", new String[]{String.valueOf(id)},
				null, null, null);
		try {
			if (c.moveToFirst()) {
				Transaction t = Transaction.fromCursor(c);
				t.systemAttributes = getSystemAttributesForTransaction(id);
				return t;
			}
		} finally {
			c.close();
		}
		return new Transaction();
	}

	public Cursor getBlotter(WhereFilter filter) {
		long t0 = System.currentTimeMillis();
		try {
			String sortOrder = getBlotterSortOrder(filter);
			return db.query(V_BLOTTER, BlotterColumns.NORMAL_PROJECTION, 
				filter.getSelection(), filter.getSelectionArgs(), null, null, 
				sortOrder);
		} finally {
			long t1 = System.currentTimeMillis();
			Log.i("DB", "getBlotter "+(t1-t0)+"ms");
		}
	}

	private String getBlotterSortOrder(WhereFilter filter) {
		String sortOrder = filter.getSortOrder();
		if (sortOrder == null || sortOrder.length() == 0) {
			sortOrder = BlotterFilter.SORT_NEWER_TO_OLDER;
		}
		return sortOrder;
	}

	public Cursor getAllTemplates(WhereFilter filter) {
		long t0 = System.currentTimeMillis();
		try {
			return db.query(V_ALL_TRANSACTIONS, BlotterColumns.NORMAL_PROJECTION, 
				filter.getSelection(), filter.getSelectionArgs(), null, null, 
				BlotterFilter.SORT_NEWER_TO_OLDER);
		} finally {
			long t1 = System.currentTimeMillis();
			Log.i("DB", "getBlotter "+(t1-t0)+"ms");
		}
	}

	public Cursor getBlotter(String where) {
		return db.query(V_BLOTTER_FOR_ACCOUNT, BlotterColumns.NORMAL_PROJECTION, where, null, null, null, 
				BlotterColumns.datetime+" DESC");
	}

	/**
	 * [Bill Filtering] Returns all the expenses (negative amount) for a given Account in a given period.
	 * @param accountId Account id.
	 * @param start Start date.
	 * @param end End date.
	 * @return Transactions (negative amount) of the given Account, from start date to end date.
	 */
	public Cursor getAllExpenses(long accountId, long startDate, long endDate) {
        // query
		String whereFrom = TransactionColumns.from_account_id+"=? AND "+TransactionColumns.from_amount+"<? AND "+
						   TransactionColumns.datetime+">? AND "+TransactionColumns.datetime+"<? AND "+
						   TransactionColumns.is_template+"!=?";
		
		String whereTo = TransactionColumns.to_account_id+"=? AND "+TransactionColumns.to_amount+"<? AND "+
						 TransactionColumns.datetime+">? AND "+TransactionColumns.datetime+"<? AND "+
						 TransactionColumns.is_template+"!=?";
		try {
			Cursor c1 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					    whereFrom, new String[]{String.valueOf(accountId), "0", 
						String.valueOf(startDate), String.valueOf(endDate), "1"}, 
						null, null, TransactionColumns.datetime.name());
			
			Cursor c2 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					    whereTo, new String[]{String.valueOf(accountId), "0", 
						String.valueOf(startDate), String.valueOf(endDate), "1"}, 
						null, null, TransactionColumns.datetime.name());
			
			MergeCursor c = new MergeCursor(new Cursor[] {c1, c2});
			return c;
		} catch(SQLiteException e) {
			return null;
		}
	}
	
	
	/**
	 * [Bill Filtering] Returns the credits (positive amount) for a given Account in a given period, excluding payments.
	 * @param accountId Account id.
	 * @param start Start date.
	 * @param end End date.
	 * @return Transactions (positive amount) of the given Account, from start date to end date.
	 */
	public Cursor getCredits(long accountId, long startDate, long endDate) {
        // query
		String whereFrom = TransactionColumns.from_account_id+"=? AND "+TransactionColumns.from_amount+">? AND "+
						   TransactionColumns.datetime+">? AND "+TransactionColumns.datetime+"<? AND "+
						   TransactionColumns.is_ccard_payment+"=? AND "+TransactionColumns.is_template+"!=?";
		
		String whereTo = TransactionColumns.to_account_id+"=? AND "+TransactionColumns.to_amount+">? AND "+
						 TransactionColumns.datetime+">? AND "+TransactionColumns.datetime+"<? AND "+
						 TransactionColumns.is_ccard_payment+"=? AND "+TransactionColumns.is_template+"!=?";
		
		try {
			Cursor c1 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					   	whereFrom, new String[]{String.valueOf(accountId), "0", 
					    String.valueOf(startDate), String.valueOf(endDate), "0", "1"}, 
					    null, null, TransactionColumns.datetime.name());
			
			Cursor c2 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					    whereTo, new String[]{String.valueOf(accountId), "0", 
						String.valueOf(startDate), String.valueOf(endDate), "0", "1"}, 
						null, null, TransactionColumns.datetime.name());
			
			MergeCursor c = new MergeCursor(new Cursor[] {c1, c2});
			return c;
		} catch(SQLiteException e) {
			return null;
		}
	}
	
	/**
	 * [Bill Filtering] Returns all the payments for a given Credit Card Account in a given period.
	 * @param accountId Account id.
	 * @param start Start date.
	 * @param end End date.
	 * @return Transactions of the given Account, from start date to end date.
	 */
	public Cursor getPayments(long accountId, long startDate, long endDate) {
        // query direct payments
		String whereFrom = TransactionColumns.from_account_id+"=? AND "+TransactionColumns.from_amount+">? AND "+
						   TransactionColumns.datetime+">? AND "+TransactionColumns.datetime+"<? AND "+
						   TransactionColumns.is_ccard_payment+"=? AND "+TransactionColumns.is_template+"!=?";
		
		String whereTo =  TransactionColumns.to_account_id+"=? AND "+TransactionColumns.to_amount+">? AND "+
						  TransactionColumns.datetime+">? AND "+TransactionColumns.datetime+"<? AND "+
						  TransactionColumns.is_ccard_payment+"=? AND "+TransactionColumns.is_template+"!=?";
		
		try {
			Cursor c1 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					   	whereFrom, new String[]{String.valueOf(accountId), "0", 
						String.valueOf(startDate), String.valueOf(endDate), "1", "1"}, 
						null, null, TransactionColumns.datetime.name());
			
			Cursor c2 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
						whereTo, new String[]{String.valueOf(accountId), "0", 
						String.valueOf(startDate), String.valueOf(endDate), "1", "1"}, 
						null, null, TransactionColumns.datetime.name());
			
			Cursor c = new MergeCursor(new Cursor[] {c1, c2});
			return c;
		} catch(SQLiteException e) {
			return null;
		}
	}
	
    /**
     * [Monthly view] Returns all the transactions for a given Account in a given period (month).
     * @param accountId Account id.
     * @param start Start date.
     * @param end End date.
     * @return Transactions (negative value) of the given Account, from start date to end date.
     */
    public Cursor getAllTransactions(long accountId, long startDate, long endDate) {
        // query
        String where = "("+TransactionColumns.from_account_id+"=? OR "+TransactionColumns.to_account_id+"=?) AND "+
        			   TransactionColumns.datetime+">? AND "+TransactionColumns.datetime+"<? AND "+
        			   TransactionColumns.is_template+"!=?";       
        try {
            Cursor c = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
                       where, new String[]{String.valueOf(accountId), String.valueOf(accountId), 
            		   String.valueOf(startDate), String.valueOf(endDate), "1"}, null, null, 
            		   TransactionColumns.datetime.name());
            return c;
        } catch(SQLiteException e) {
            return null;
        }
    }
    
	public Cursor getTransactions(WhereFilter filter) {
		String sortOrder = getBlotterSortOrder(filter);
		return db.query(V_BLOTTER_FOR_ACCOUNT, BlotterColumns.NORMAL_PROJECTION, 
				filter.getSelection(), filter.getSelectionArgs(), null, null, 
				sortOrder);
	}
	
	public Total[] getTransactionsBalance(WhereFilter filter) {
		Cursor c = db.query(V_BLOTTER_FOR_ACCOUNT, BlotterColumns.BALANCE_PROJECTION, 
				filter.getSelection(), filter.getSelectionArgs(), 
				BlotterColumns.BALANCE_GROUP_BY, null, null);
		try {			
			int count = c.getCount();
			List<Total> totals = new ArrayList<Total>(count);
			while (c.moveToNext()) {	
				long currencyId = c.getLong(0);
				long balance = c.getLong(1);
				Total total = new Total(CurrencyCache.getCurrency(currencyId));
				total.balance = balance; 
				totals.add(total);
			}
			return totals.toArray(new Total[totals.size()]);
		} finally {
			c.close();
		}
	}

	private static final String ACCOUNT_TOTAL_AMOUNT_UPDATE = "UPDATE "+ACCOUNT_TABLE
	+" SET "+AccountColumns.TOTAL_AMOUNT+"="+AccountColumns.TOTAL_AMOUNT+"+(?) "
	+" WHERE "+AccountColumns.ID+"=?";

	private void updateAccountTotalAmount(long accountId, long amount) {
		db.execSQL(ACCOUNT_TOTAL_AMOUNT_UPDATE, new Object[]{amount, accountId});
	}

	private static final String LOCATION_COUNT_UPDATE = "UPDATE "+LOCATIONS_TABLE
	+" SET count=count+(?) WHERE _id=?";

	private void updateLocationCount(long locationId, int count) {
		db.execSQL(LOCATION_COUNT_UPDATE, new Object[]{count, locationId});
	}

	private static final String ACCOUNT_LAST_ACCOUNT_UPDATE = "UPDATE "+ACCOUNT_TABLE
	+" SET "+AccountColumns.LAST_ACCOUNT_ID+"=? "
	+" WHERE "+AccountColumns.ID+"=?";

	private static final String ACCOUNT_LAST_CATEGORY_UPDATE = "UPDATE "+ACCOUNT_TABLE
	+" SET "+AccountColumns.LAST_CATEGORY_ID+"=? "
	+" WHERE "+AccountColumns.ID+"=?";

	private static final String CATEGORY_LAST_LOCATION_UPDATE = "UPDATE "+CATEGORY_TABLE
	+" SET last_location_id=(?) WHERE _id=?";

	private static final String CATEGORY_LAST_PROJECT_UPDATE = "UPDATE "+CATEGORY_TABLE
	+" SET last_project_id=(?) WHERE _id=?";

	private void updateLastUsed(Transaction t) {
		if (t.isTransfer()) {
			db.execSQL(ACCOUNT_LAST_ACCOUNT_UPDATE, new Object[]{t.toAccountId, t.fromAccountId});
		}
		db.execSQL(ACCOUNT_LAST_CATEGORY_UPDATE, new Object[]{t.categoryId, t.fromAccountId});
		db.execSQL(CATEGORY_LAST_LOCATION_UPDATE, new Object[]{t.locationId, t.categoryId});
		db.execSQL(CATEGORY_LAST_PROJECT_UPDATE, new Object[]{t.projectId, t.categoryId});
	}
	
	public long duplicateTransaction(long id) {
		return duplicateTransaction(id, 0, 1);
	}
	
	public long duplicateTransactionWithMultiplier(long id, int multiplier) {
		return duplicateTransaction(id, 0, multiplier);
	}
	
	public long duplicateTransactionAsTemplate(long id) {
		return duplicateTransaction(id, 1, 1);
	}

	private long duplicateTransaction(long id, int isTemplate, int multiplier) {
		db.beginTransaction();
		try {
			long now = System.currentTimeMillis();
			Transaction transaction = getTransaction(id);
			transaction.lastRecurrence = now;
			updateTransaction(transaction);
			transaction.id = -1;
			transaction.isTemplate = isTemplate;
			transaction.dateTime = now;
			if (isTemplate == 0) {
				transaction.recurrence = null;
				transaction.notificationOptions = null;
			}
			if (multiplier > 1) {
				transaction.fromAmount *= multiplier;
				transaction.toAmount *= multiplier;
			}
			HashMap<Long, String> attributesMap = getAllAttributesForTransaction(id);
			LinkedList<TransactionAttribute> attributes = new LinkedList<TransactionAttribute>();
			for (long attributeId : attributesMap.keySet()) {
				TransactionAttribute ta = new TransactionAttribute();
				ta.attributeId = attributeId;
				ta.value = attributesMap.get(attributeId);
				attributes.add(ta);
			}
			long transactionId = insertTransaction(transaction);
			if (attributes.size() > 0) {
				insertAttributes(transactionId, attributes);
			}
			db.setTransactionSuccessful();
			return transactionId;
		} finally {
			db.endTransaction();
		}
	}

	public long insertOrUpdate(Transaction transaction, LinkedList<TransactionAttribute> attributes) {
		db.beginTransaction();
		try {
			long transactionId;
			transaction.lastRecurrence = System.currentTimeMillis();
			if (transaction.id == -1) {
				transactionId = insertTransaction(transaction);
			} else {
				updateTransaction(transaction);
				transactionId = transaction.id;
				db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID+"=?", 
						new String[]{String.valueOf(transactionId)});
			}
			if (attributes != null) {
				insertAttributes(transactionId, attributes);
			}
			db.setTransactionSuccessful();
			return transactionId;
		} finally {
			db.endTransaction();
		}
	}

	private void insertAttributes(long transactionId, LinkedList<TransactionAttribute> attributes) {		
		for (TransactionAttribute a : attributes) {
			a.transactionId = transactionId;
			ContentValues values = a.toValues();
			db.insert(TRANSACTION_ATTRIBUTE_TABLE, null, values);
		}
	}

	private long insertTransaction(Transaction t) {
		long id = db.insert(TRANSACTION_TABLE, null, t.toValues());
		if (t.isNotTemplateLike()) {
			updateAccountTotalAmount(t.fromAccountId, t.fromAmount);
			updateAccountTotalAmount(t.toAccountId, t.toAmount);
			updateLocationCount(t.locationId, 1);
			updateLastUsed(t);
		}
		return id;
	}

    public long insertPayee(String payee) {
        if (Utils.isEmpty(payee)) {
            return 0;
        } else {
            Payee p = em.insertPayee(payee);
            return p.id;
        }
    }

    private void updateTransaction(Transaction t) {
		if (t.isNotTemplateLike()) {
			Transaction oldT = getTransaction(t.id);
			updateAccountTotalAmount(oldT.fromAccountId, oldT.fromAmount, t.fromAccountId, t.fromAmount);
			updateAccountTotalAmount(oldT.toAccountId, oldT.toAmount, t.toAccountId, t.toAmount);
			if (oldT.locationId != t.locationId) {			
				updateLocationCount(oldT.locationId, -1);
				updateLocationCount(t.locationId, 1);
			}
		}
		db.update(TRANSACTION_TABLE, t.toValues(), TransactionColumns._id +"=?",
				new String[]{String.valueOf(t.id)});		
	}
	
	private void updateAccountTotalAmount(long oldAccountId, long oldAmount, long newAccountId, long newAmount) {
		if (oldAccountId == newAccountId) {
			updateAccountTotalAmount(newAccountId, newAmount-oldAmount);
		} else {
			updateAccountTotalAmount(oldAccountId, -oldAmount);
			updateAccountTotalAmount(newAccountId, newAmount);
		}
	}

	public void deleteTransaction(long id) {
		db.beginTransaction();
		try {
			Transaction t = getTransaction(id);
			if (t.isNotTemplateLike()) {
				updateAccountTotalAmount(t.fromAccountId, -t.fromAmount);
				updateAccountTotalAmount(t.toAccountId, -t.toAmount);
				updateLocationCount(t.locationId, -1);
			}
			String[] sid = new String[]{String.valueOf(id)};
			db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID+"=?", sid);
			db.delete(TRANSACTION_TABLE, TransactionColumns._id +"=?", sid);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();			
		}
	}
	
	public void deleteTransactionNoDbTransaction(long id) {
		Transaction t = getTransaction(id);
		if (t.isNotTemplateLike()) {
			updateAccountTotalAmount(t.fromAccountId, -t.fromAmount);
			updateAccountTotalAmount(t.toAccountId, -t.toAmount);
			updateLocationCount(t.locationId, -1);
		}
		String[] sid = new String[]{String.valueOf(id)};
		db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID+"=?", sid);
		db.delete(TRANSACTION_TABLE, TransactionColumns._id +"=?", sid);
	}

	// ===================================================================
	// CATEGORY
	// ===================================================================

	public long insertOrUpdate(Category category, ArrayList<Attribute> attributes) {
		db.beginTransaction();
		try {
			long id;
			if (category.id == -1) {
				id = insertCategory(category);
			} else {
				updateCategory(category);
				id = category.id;
			}
			addAttributes(id, attributes);
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
	}
	
	private void addAttributes(long categoryId, ArrayList<Attribute> attributes) {
		db.delete(CATEGORY_ATTRIBUTE_TABLE, CategoryAttributeColumns.CATEGORY_ID+"=?", new String[]{String.valueOf(categoryId)});
		ContentValues values = new ContentValues();
		values.put(CategoryAttributeColumns.CATEGORY_ID, categoryId);
		for (Attribute a : attributes) {
			values.put(CategoryAttributeColumns.ATTRIBUTE_ID, a.id);
			db.insert(CATEGORY_ATTRIBUTE_TABLE, null, values);
		}
	}

	private long insertCategory(Category category) {	
		long parentId = category.getParentId();
		String categoryTitle = category.title;
		List<Category> subordinates = getSubordinates(parentId);
		if (subordinates.isEmpty()) {
			return insertChildCategory(parentId, category);
		} else {
			long mateId = -1;
			for (Category c : subordinates) {
				if (categoryTitle.compareTo(c.title) <= 0) {
					break;
				}
				mateId = c.id;
			}
			if (mateId == -1) {
				return insertChildCategory(parentId, category);
			} else {
				return insertMateCategory(mateId, category);
			}
		}
	}

	private long updateCategory(Category category) {
		Category oldCategory = getCategory(category.id);
		if (oldCategory.getParentId() == category.getParentId()) {
			updateCategory(category.id, category.title, category.type);
            updateChildCategoriesType(category.type, category.left, category.right);
		} else {
			moveCategory(category.id, category.getParentId(), category.title, category.type);
		}
		return category.id;
	}

	private static final String GET_PARENT_SQL = "(SELECT "
		+ "parent."+CategoryColumns._id+" AS "+CategoryColumns._id
		+ " FROM "
		+ CATEGORY_TABLE+" AS node"+","
		+ CATEGORY_TABLE+" AS parent "
		+" WHERE "
		+" node."+CategoryColumns.left+" BETWEEN parent."+CategoryColumns.left+" AND parent."+CategoryColumns.right
		+" AND node."+CategoryColumns._id+"=?"
		+" AND parent."+CategoryColumns._id+"!=?"
		+" ORDER BY parent."+CategoryColumns.left+" DESC)";
	
	public Category getCategory(long id) {
		Cursor c = db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, 
				CategoryViewColumns._id+"=?", new String[]{String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToNext()) {				
				Category cat = new Category();
				cat.id = id;
				cat.title = c.getString(CategoryViewColumns.title.ordinal());
				cat.level = c.getInt(CategoryViewColumns.level.ordinal());
				cat.left = c.getInt(CategoryViewColumns.left.ordinal());
				cat.right = c.getInt(CategoryViewColumns.right.ordinal());
                cat.type = c.getInt(CategoryViewColumns.type.ordinal());
				String s = String.valueOf(id);
				Cursor c2 = db.query(GET_PARENT_SQL, new String[]{CategoryColumns._id.name()}, null, new String[]{s,s},
						null, null, null, "1");
				try {
					if (c2.moveToFirst()) {
						cat.parent = new Category(c2.getLong(0));
					}
				} finally {
					c2.close();
				}
				return cat;
			} else {
				return new Category(-1);
			}
		} finally {
			c.close();
		}
	}

	public Category getCategoryByLeft(long left) {
		Cursor c = db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, 
				CategoryViewColumns.left+"=?", new String[]{String.valueOf(left)}, null, null, null);
		try {
			if (c.moveToNext()) {				
				return Category.formCursor(c);
			} else {
				return new Category(-1);
			}
		} finally {
			c.close();
		}
	}

	public CategoryTree<Category> getAllCategoriesTree(boolean includeNoCategory) {
		Cursor c = getAllCategories(includeNoCategory);
		try { 
			CategoryTree<Category> tree = CategoryTree.createFromCursor(c, new NodeCreator<Category>(){
				@Override
				public Category createNode(Cursor c) {
					return Category.formCursor(c);
				}				
			});
			return tree;
		} finally {
			c.close();
		}
	}
	
	public HashMap<Long, Category> getAllCategoriesMap(boolean includeNoCategory) {
		return getAllCategoriesTree(includeNoCategory).asMap();
	}

	public ArrayList<Category> getAllCategoriesList(boolean includeNoCategory) {
		ArrayList<Category> list = new ArrayList<Category>();
		Cursor c = getAllCategories(includeNoCategory);
		try { 
			while (c.moveToNext()) {
				Category category = Category.formCursor(c);
				list.add(category);
			}
		} finally {
			c.close();
		}
		return list;
	}

	public Cursor getAllCategories(boolean includeNoCategory) {
		return db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, 
				includeNoCategory ? null : CategoryViewColumns._id+"!=0", null, null, null, null);
	}
	
	public Cursor getAllCategoriesWithoutSubtree(long id) {
		long left = 0, right = 0;
		Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.left.name(), CategoryColumns.right.name()},
				CategoryColumns._id+"=?", new String[]{String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToFirst()) {
				left = c.getLong(0);
				right = c.getLong(1);
			}
		} finally {
			c.close();
		}
		return db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, 
				"NOT ("+CategoryViewColumns.left+">="+left+" AND "+CategoryColumns.right+"<="+right+")", null, null, null, null);
	}

	private static final String INSERT_CATEGORY_UPDATE_RIGHT = "UPDATE "+CATEGORY_TABLE+" SET "+CategoryColumns.right+"="+CategoryColumns.right+"+2 WHERE "+CategoryColumns.right+">?";
	private static final String INSERT_CATEGORY_UPDATE_LEFT = "UPDATE "+CATEGORY_TABLE+" SET "+CategoryColumns.left+"="+CategoryColumns.left+"+2 WHERE "+CategoryColumns.left+">?";
	
	public long insertChildCategory(long parentId, Category category) {
		//DECLARE v_leftkey INT UNSIGNED DEFAULT 0;
		//SELECT l INTO v_leftkey FROM `nset` WHERE `id` = ParentID;
		//UPDATE `nset` SET `r` = `r` + 2 WHERE `r` > v_leftkey;
		//UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_leftkey;
		//INSERT INTO `nset` (`name`, `l`, `r`) VALUES (NodeName, v_leftkey + 1, v_leftkey + 2);
        int type = getActualCategoryType(parentId, category);
		return insertCategory(CategoryColumns.left.name(), parentId, category.title, type);
	}

    public long insertMateCategory(long categoryId, Category category) {
		//DECLARE v_rightkey INT UNSIGNED DEFAULT 0;
		//SELECT `r` INTO v_rightkey FROM `nset` WHERE `id` = MateID;
		//UPDATE `	nset` SET `r` = `r` + 2 WHERE `r` > v_rightkey;
		//UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_rightkey;
		//INSERT `nset` (`name`, `l`, `r`) VALUES (NodeName, v_rightkey + 1, v_rightkey + 2);
        Category mate = getCategory(categoryId);
        long parentId = mate.getParentId();
        int type = getActualCategoryType(parentId, category);
		return insertCategory(CategoryColumns.right.name(), categoryId, category.title, type);
	}

    private int getActualCategoryType(long parentId, Category category) {
        int type = category.type;
        if (parentId > 0) {
            Category parent = getCategory(parentId);
            type = parent.type;
        }
        return type;
    }

	private long insertCategory(String field, long categoryId, String title, int type) {
		int num = 0;
		Cursor c = db.query(CATEGORY_TABLE, new String[]{field},
				CategoryColumns._id+"=?", new String[]{String.valueOf(categoryId)}, null, null, null);
		try {
			if (c.moveToFirst()) {
				num = c.getInt(0);
			}
		} finally  {
			c.close();
		}
        String[] args = new String[]{String.valueOf(num)};
        db.execSQL(INSERT_CATEGORY_UPDATE_RIGHT, args);
        db.execSQL(INSERT_CATEGORY_UPDATE_LEFT, args);
        ContentValues values = new ContentValues();
        values.put(CategoryColumns.title.name(), title);
        int left = num + 1;
        int right = num + 2;
        values.put(CategoryColumns.left.name(), left);
        values.put(CategoryColumns.right.name(), right);
        values.put(CategoryColumns.type.name(), type);
        long id = db.insert(CATEGORY_TABLE, null, values);
        updateChildCategoriesType(type, left, right);
    	return id;
	}

    private static final String CATEGORY_UPDATE_CHILDREN_TYPES = "UPDATE "+CATEGORY_TABLE+" SET "+CategoryColumns.type+"=? WHERE "+CategoryColumns.left+">? AND "+CategoryColumns.right+"<?";

    private void updateChildCategoriesType(int type, int left, int right) {
        db.execSQL(CATEGORY_UPDATE_CHILDREN_TYPES, new Object[]{type, left, right});
    }

    private static final String V_SUBORDINATES = "(SELECT "
	+"node."+CategoryColumns._id+" as "+CategoryViewColumns._id+", "
	+"node."+CategoryColumns.title+" as "+CategoryViewColumns.title+", "
	+"(COUNT(parent."+CategoryColumns._id+") - (sub_tree.depth + 1)) AS "+CategoryViewColumns.level
	+" FROM "
	+CATEGORY_TABLE+" AS node, "
	+CATEGORY_TABLE+" AS parent, "
	+CATEGORY_TABLE+" AS sub_parent, "
	+"("
		+"SELECT node."+CategoryColumns._id+" as "+CategoryColumns._id+", "
		+"(COUNT(parent."+CategoryColumns._id+") - 1) AS depth"
		+" FROM "
		+CATEGORY_TABLE+" AS node, "
		+CATEGORY_TABLE+" AS parent "
		+" WHERE node."+CategoryColumns.left+" BETWEEN parent."+CategoryColumns.left+" AND parent."+CategoryColumns.right
		+" AND node."+CategoryColumns._id+"=?"
		+" GROUP BY node."+CategoryColumns._id
		+" ORDER BY node."+CategoryColumns.left
	+") AS sub_tree "
	+" WHERE node."+CategoryColumns.left+" BETWEEN parent."+CategoryColumns.left+" AND parent."+CategoryColumns.right
	+" AND node."+CategoryColumns.left+" BETWEEN sub_parent."+CategoryColumns.left+" AND sub_parent."+CategoryColumns.right
	+" AND sub_parent."+CategoryColumns._id+" = sub_tree."+CategoryColumns._id
	+" GROUP BY node."+CategoryColumns._id
	+" HAVING "+CategoryViewColumns.level+"=1"
	+" ORDER BY node."+CategoryColumns.left
	+")";
	
	public List<Category> getSubordinates(long parentId) {
		List<Category> list = new LinkedList<Category>();
		Cursor c = db.query(V_SUBORDINATES, new String[]{CategoryViewColumns._id.name(), CategoryViewColumns.title.name(), CategoryViewColumns.level.name()}, null,
				new String[]{String.valueOf(parentId)}, null, null, null);
		//DatabaseUtils.dumpCursor(c);
		try {
			while (c.moveToNext()) {
				long id = c.getLong(0);
				String title = c.getString(1);
				Category cat = new Category();
				cat.id = id;
				cat.title = title;
				list.add(cat);
			}
		} finally {
			c.close();
		}
		return list;
	}
	
	private static final String DELETE_CATEGORY_UPDATE1 = "UPDATE "+TRANSACTION_TABLE
		+" SET "+TransactionColumns.category_id +"=0 WHERE "
		+TransactionColumns.category_id +" IN ("
		+"SELECT "+CategoryColumns._id+" FROM "+CATEGORY_TABLE+" WHERE "
		+CategoryColumns.left+" BETWEEN ? AND ?)";
	private static final String DELETE_CATEGORY_UPDATE2 = "UPDATE "+CATEGORY_TABLE
		+" SET "+CategoryColumns.left+"=(CASE WHEN "+CategoryColumns.left+">%s THEN "
		+CategoryColumns.left+"-%s ELSE "+CategoryColumns.left+" END),"
		+CategoryColumns.right+"="+CategoryColumns.right+"-%s"
		+" WHERE "+CategoryColumns.right+">%s";

	public void deleteCategory(long categoryId) {
		//DECLARE v_leftkey, v_rightkey, v_width INT DEFAULT 0;
		//
		//SELECT
		//	`l`, `r`, `r` - `l` + 1 INTO v_leftkey, v_rightkey, v_width
		//FROM `nset`
		//WHERE
		//	`id` = NodeID;
		//
		//DELETE FROM `nset` WHERE `l` BETWEEN v_leftkey AND v_rightkey;
		//
		//UPDATE `nset`
		//SET
		//	`l` = IF(`l` > v_leftkey, `l` - v_width, `l`),
		//	`r` = `r` - v_width
		//WHERE
		//	`r` > v_rightkey;
		int left = 0, right = 0;
		Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.left.name(), CategoryColumns.right.name()},
				CategoryColumns._id+"=?", new String[]{String.valueOf(categoryId)}, null, null, null);
		try {
			if (c.moveToFirst()) {
				left = c.getInt(0);
				right = c.getInt(1);
			}
		} finally  {
			c.close();
		}
		db.beginTransaction();
		try {
			int width = right - left + 1;
			String[] args = new String[]{String.valueOf(left), String.valueOf(right)};
			db.execSQL(DELETE_CATEGORY_UPDATE1, args);
			db.delete(CATEGORY_TABLE, CategoryColumns.left+" BETWEEN ? AND ?", args);
			db.execSQL(String.format(DELETE_CATEGORY_UPDATE2, left, width, width, right));
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	private void updateCategory(long id, String title, int type) {
		ContentValues values = new ContentValues();
		values.put(CategoryColumns.title.name(), title);
        values.put(CategoryColumns.type.name(), type);
		db.update(CATEGORY_TABLE, values, CategoryColumns._id+"=?", new String[]{String.valueOf(id)});
	}
	
	public void updateCategoryTree(CategoryTree<Category> tree) {
		db.beginTransaction();
		try {
			updateCategoryTreeInTransaction(tree);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	private static final String WHERE_CATEGORY_ID = CategoryColumns._id+"=?";
	
	private void updateCategoryTreeInTransaction(CategoryTree<Category> tree) {
		ContentValues values = new ContentValues();
		String[] sid = new String[1];
		for (Category c : tree) {
			values.put(CategoryColumns.left.name(), c.left);
			values.put(CategoryColumns.right.name(), c.right);
			sid[0] = String.valueOf(c.id);
			db.update(CATEGORY_TABLE, values, WHERE_CATEGORY_ID, sid);
			if (c.hasChildren()) {
				updateCategoryTreeInTransaction(c.children);
			}
		}
	}

	public void moveCategory(long id, long newParentId, String title, int type) {
        updateCategory(id, title, type);

        long originLft, originRgt;
        Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.left.name(), CategoryColumns.right.name()},
                CategoryColumns._id+"=?", new String[]{String.valueOf(id)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                originLft = c.getLong(0);
                originRgt = c.getLong(1);
            } else {
                return;
            }
        } finally {
            c.close();
        }

        c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.right.name(), CategoryColumns.type.name()},
                CategoryColumns._id+"=?", new String[]{String.valueOf(newParentId)}, null, null, null);
        long newParentRgt;
        int newParentType;
        try {
            if (c.moveToFirst()) {
                newParentRgt = c.getLong(0);
                newParentType = c.getInt(1);
            } else {
                return;
            }
        } finally {
            c.close();
        }

        db.execSQL("UPDATE "+CATEGORY_TABLE+" SET "
            +CategoryColumns.left+" = "+CategoryColumns.left+" + CASE "
            +" WHEN "+newParentRgt+" < "+originLft
            +" THEN CASE "
            +" WHEN "+CategoryColumns.left+" BETWEEN "+originLft+" AND "+originRgt
            +" THEN "+newParentRgt+" - "+originLft
            +" WHEN "+CategoryColumns.left+" BETWEEN "+newParentRgt+" AND "+(originLft-1)
            +" THEN "+(originRgt-originLft+1)
            +" ELSE 0 END "
            +" WHEN "+newParentRgt+" > "+originRgt
            +" THEN CASE "
            +" WHEN "+CategoryColumns.left+" BETWEEN "+originLft+" AND "+originRgt
            +" THEN "+(newParentRgt-originRgt-1)
            +" WHEN "+CategoryColumns.left+" BETWEEN "+(originRgt+1)+" AND "+(newParentRgt-1)
            +" THEN "+(originLft - originRgt - 1)
            +" ELSE 0 END "
            +" ELSE 0 END,"
            +CategoryColumns.right+" = "+CategoryColumns.right+" + CASE "
            +" WHEN "+newParentRgt+" < "+originLft
            +" THEN CASE "
            +" WHEN "+CategoryColumns.right+" BETWEEN "+originLft+" AND "+originRgt
            +" THEN "+(newParentRgt-originLft)
            +" WHEN "+CategoryColumns.right+" BETWEEN "+newParentRgt+" AND "+(originLft - 1)
            +" THEN "+(originRgt-originLft+1)
            +" ELSE 0 END "
            +" WHEN "+newParentRgt+" > "+originRgt
            +" THEN CASE "
            +" WHEN "+CategoryColumns.right+" BETWEEN "+originLft+" AND "+originRgt
            +" THEN "+(newParentRgt-originRgt-1)
            +" WHEN "+CategoryColumns.right+" BETWEEN "+(originRgt+1)+" AND "+(newParentRgt-1)
            +" THEN "+(originLft-originRgt-1)
            +" ELSE 0 END "
            +" ELSE 0 END");

        c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.left.name(), CategoryColumns.right.name()},
                CategoryColumns._id+"=?", new String[]{String.valueOf(id)}, null, null, null);
        int newLeft, newRight;
        try {
            if (c.moveToFirst()) {
                newLeft = c.getInt(0);
                newRight = c.getInt(1);
            } else {
                return;
            }
        } finally {
            c.close();
        }

        int newType = type;
        if (newParentId > 0) {
            updateCategoryType(id, newParentType);
            newType = newParentType;
        }
        updateChildCategoriesType(newType, newLeft, newRight);
	}

    private static final String UPDATE_CATEGORY_TYPE = "UPDATE "+CATEGORY_TABLE+" SET "+CategoryColumns.type+"=? WHERE "+CategoryColumns._id+"=?";

    private void updateCategoryType(long id, int type) {
        db.execSQL(UPDATE_CATEGORY_TYPE, new Object[]{type, id});
    }

    // ===================================================================
	// ATTRIBUTES
	// ===================================================================

	public ArrayList<Attribute> getAttributesForCategory(long categoryId) {
		Cursor c = db.query(V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION, 
				CategoryAttributeColumns.CATEGORY_ID+"=?", new String[]{String.valueOf(categoryId)}, 
				null, null, AttributeColumns.NAME);
		try {
			ArrayList<Attribute> list = new ArrayList<Attribute>(c.getCount());
			while (c.moveToNext()) {
				Attribute a = Attribute.fromCursor(c);
				list.add(a);
			}
			return list;
		} finally {
			c.close();
		}		
	}

	public ArrayList<Attribute> getAllAttributesForCategory(long categoryId) {
		Category category = getCategory(categoryId);
		Cursor c = db.query(V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION, 
				AttributeViewColumns.CATEGORY_LEFT+"<= ? AND "+AttributeViewColumns.CATEGORY_RIGHT+" >= ?", 
				new String[]{String.valueOf(category.left), String.valueOf(category.right)}, 
				null, null, AttributeColumns.NAME);
		try {
			ArrayList<Attribute> list = new ArrayList<Attribute>(c.getCount());
			while (c.moveToNext()) {
				Attribute a = Attribute.fromCursor(c);
				list.add(a);
			}
			return list;
		} finally {
			c.close();
		}		
	}
	
	public Attribute getSystemAttribute(SystemAttribute a) {
		Attribute sa = getAttribute(a.id);
		sa.name = context.getString(a.titleId);
		return sa;
	}

	public Attribute getAttribute(long id) {
		Cursor c = db.query(ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION, 
				AttributeColumns.ID+"=?", new String[]{String.valueOf(id)}, 
				null, null, null);
		try {
			if (c.moveToFirst()) {
				return Attribute.fromCursor(c);
			}
		} finally {
			c.close();
		}
		return new Attribute(-1); 
	}

	public long insertOrUpdate(Attribute attribute) {
		if (attribute.id == -1) {
			return insertAttribute(attribute);
		} else {
			updateAttribute(attribute);
			return attribute.id;
		}
	}

	public void deleteAttribute(long id) {
		db.beginTransaction();
		try {
			String[] p = new String[]{String.valueOf(id)};
			db.delete(ATTRIBUTES_TABLE, AttributeColumns.ID+"=?", p);
			db.delete(CATEGORY_ATTRIBUTE_TABLE, CategoryAttributeColumns.ATTRIBUTE_ID+"=?", p);
			db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.ATTRIBUTE_ID+"=?", p);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private long insertAttribute(Attribute attribute) {
		ContentValues values = attribute.toValues();
		return db.insert(ATTRIBUTES_TABLE, null, values);
	}

	private void updateAttribute(Attribute attribute) {
		ContentValues values = attribute.toValues();
		db.update(ATTRIBUTES_TABLE, values, AttributeColumns.ID+"=?", new String[]{String.valueOf(attribute.id)});
	}

	public Cursor getAllAttributes() {
		return db.query(ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION, 
				AttributeColumns.ID+">0", null, null, null, AttributeColumns.NAME);
	}

	public HashMap<Long, String> getAllAttributesMap() {
		Cursor c = db.query(V_ATTRIBUTES, AttributeViewColumns.NORMAL_PROJECTION, null, null, null, null, 
				AttributeViewColumns.CATEGORY_ID+", "+AttributeViewColumns.NAME);
		try {
			HashMap<Long, String> attributes = new HashMap<Long, String>();
			StringBuilder sb = null;
			long prevCategoryId = -1;
			while (c.moveToNext()) {
				long categoryId = c.getLong(AttributeViewColumns.Indicies.CATEGORY_ID);
				String name = c.getString(AttributeViewColumns.Indicies.NAME);
				if (prevCategoryId != categoryId) {
					if (sb != null) {
						attributes.put(prevCategoryId, sb.append("]").toString());
						sb.setLength(1);
					} else {
						sb = new StringBuilder();
						sb.append("[");
					}					
					prevCategoryId = categoryId;
				}
				if (sb.length() > 1) {
					sb.append(", ");
				}
				sb.append(name);
			}
			if (sb != null) {
				attributes.put(prevCategoryId, sb.append("]").toString());
			}
			return attributes;
		} finally {
			c.close();
		}
	}

	public HashMap<Long, String> getAllAttributesForTransaction(long transactionId) {
		Cursor c = db.query(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION, 
				TransactionAttributeColumns.TRANSACTION_ID+"=? AND "+TransactionAttributeColumns.ATTRIBUTE_ID+">=0", 
				new String[]{String.valueOf(transactionId)}, 
				null, null, null);
		try {
			HashMap<Long, String> attributes = new HashMap<Long, String>();
			while (c.moveToNext()) {
				long attributeId = c.getLong(TransactionAttributeColumns.Indicies.ATTRIBUTE_ID);
				String value = c.getString(TransactionAttributeColumns.Indicies.VALUE);
				attributes.put(attributeId, value);
			}
			return attributes;
		} finally {
			c.close();
		}		
	}

	public EnumMap<SystemAttribute, String> getSystemAttributesForTransaction(long transactionId) {
		Cursor c = db.query(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION, 
				TransactionAttributeColumns.TRANSACTION_ID+"=? AND "+TransactionAttributeColumns.ATTRIBUTE_ID+"<0", 
				new String[]{String.valueOf(transactionId)}, 
				null, null, null);
		try {
			EnumMap<SystemAttribute, String> attributes = new EnumMap<SystemAttribute, String>(SystemAttribute.class);
			while (c.moveToNext()) {
				long attributeId = c.getLong(TransactionAttributeColumns.Indicies.ATTRIBUTE_ID);
				String value = c.getString(TransactionAttributeColumns.Indicies.VALUE);
				attributes.put(SystemAttribute.forId(attributeId), value);
			}
			return attributes;
		} finally {
			c.close();
		}		
	}
	
	
	/**
	 * Gets the location name for a given id.
	 * @param id
	 * @return
	 */
	public String getLocationName(long id) {
		Cursor c = db.query(LOCATIONS_TABLE, new String[]{LocationColumns.NAME}, 
				LocationColumns.ID+"=?", new String[]{String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToNext()) {
				return c.getString(0);
			} else {
				return "";
			}
		} finally {
			c.close();
		}
	}

	public void clearAll(long[] ids) {
		String sql = "UPDATE "+TRANSACTION_TABLE+" SET "+TransactionColumns.status +"='"+TransactionStatus.CL+"'";
		runInTransaction(sql, ids);
	}

	public void reconcileAll(long[] ids) {
		String sql = "UPDATE "+TRANSACTION_TABLE+" SET "+TransactionColumns.status +"='"+TransactionStatus.RC+"'";
		runInTransaction(sql, ids);
	}

	public void deleteAll(long[] ids) {
		db.beginTransaction();
		try {
			for (long id : ids) {
				deleteTransactionNoDbTransaction(id);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private void runInTransaction(String sql, long[] ids) {
		db.beginTransaction();
		try {
			int count = ids.length;
			int bucket = 100;
			int num = 1+count/bucket;
			for (int i=0; i<num; i++) {
				int x = bucket*i;
				int y = Math.min(count, bucket*(i+1));
				String script = createSql(sql, ids, x, y);
				db.execSQL(script);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private String createSql(String updateSql, long[] ids, int x, int y) {
		StringBuilder sb = new StringBuilder(updateSql)
								.append(" WHERE ")
								.append(TransactionColumns.is_template)
								.append("=0 AND ")
								.append(TransactionColumns._id)
								.append(" IN (");
		for (int i=x; i<y; i++) {
			if (i > x) {
				sb.append(",");
			}
			sb.append(ids[i]);
		}
		sb.append(")");
		return sb.toString();
	}
	
	private static final String UPDATE_LAST_RECURRENCE = 
		"UPDATE "+TRANSACTION_TABLE+" SET "+TransactionColumns.last_recurrence +"=? WHERE "+TransactionColumns._id +"=?";

	public void storeMissedSchedules(List<RestoredTransaction> restored, long now) {
		db.beginTransaction();
		try {
			HashMap<Long, Transaction> transactions = new HashMap<Long, Transaction>();
			for (RestoredTransaction rt : restored) {
				long transactionId = rt.transactionId;
				Transaction t = transactions.get(transactionId);
				if (t == null) {
					t = getTransaction(transactionId);
					transactions.put(transactionId, t);
				}
				t.id = -1;
				t.dateTime = rt.dateTime.getTime();
				t.status = TransactionStatus.RS;
				t.isTemplate = 0;
				insertTransaction(t);
				t.id = transactionId;
			}
			for (Transaction t : transactions.values()) {
				db.execSQL(UPDATE_LAST_RECURRENCE, new Object[]{now, t.id});		
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * @param accountId
	 * @param period
	 * @return
	 */
	public int getCustomClosingDay(long accountId, int period) {
		String where = CreditCardClosingDateColumns.ACCOUNT_ID+"=? AND "+
					   CreditCardClosingDateColumns.PERIOD+"=?";
		
		Cursor c = db.query(CCARD_CLOSING_DATE_TABLE, new String[] {CreditCardClosingDateColumns.CLOSING_DAY}, 
			    where, new String[]{Long.toString(accountId), Integer.toString(period)}, null, null, null);
		
		int res = 0;
		try {
			if (c!=null) {
				if (c.getCount()>0) {
					c.moveToFirst();
					res = c.getInt(0);
				} else {
					res = 0;
				}
			} else {
				// there is no custom closing day in database for the given account id an period
				res =  0;
			}
		} catch(SQLiteException e) {
			res = 0;
		} finally {
			c.close();
		}
		return res;
	}
	

	/**
	 * @param accountId
	 * @param period
	 * @param closingDay
	 */
	public void setCustomClosingDay(long accountId, int period, int closingDay) {
		ContentValues values = new ContentValues();
        values.put(CreditCardClosingDateColumns.ACCOUNT_ID, Long.toString(accountId));
        values.put(CreditCardClosingDateColumns.PERIOD, Integer.toString(period));
        values.put(CreditCardClosingDateColumns.CLOSING_DAY, Integer.toString(closingDay));
		db.insert(CCARD_CLOSING_DATE_TABLE, null, values);
	}
	
	/**
	 * 
	 * @param accountId
	 * @param period
	 */
	public void deleteCustomClosingDay(long accountId, int period) {
		String where = CreditCardClosingDateColumns.ACCOUNT_ID+"=? AND "+
		   			   CreditCardClosingDateColumns.PERIOD+"=?";
		String[] args = new String[] {Long.toString(accountId), Integer.toString(period)};
		db.delete(CCARD_CLOSING_DATE_TABLE, where, args);
	}
	
	/**
	 * @param accountId
	 * @param period
	 * @param closingDay
	 */
	public void updateCustomClosingDay(long accountId, int period, int closingDay) {
		// delete previous content
		deleteCustomClosingDay(accountId, period);
		
		// save new value
		setCustomClosingDay(accountId, period, closingDay);
	}
}

