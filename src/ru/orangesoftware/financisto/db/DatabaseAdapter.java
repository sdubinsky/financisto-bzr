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

import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.ATTRIBUTES_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CATEGORY_ATTRIBUTE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CATEGORY_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.LOCATIONS_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.TRANSACTION_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_ALL_TRANSACTIONS;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_ATTRIBUTES;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_CATEGORY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.AttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.AttributeViewColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryAttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryViewColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionAttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.SystemAttribute;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionAttribute;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class DatabaseAdapter {

	private final Context context;
	private final DatabaseHelper dbHelper;
	
	private SQLiteDatabase db;
	
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
	}

	public void close() {
		db.close();
	}
	
	public SQLiteDatabase db() {
		return db;
	}

	// ===================================================================
	// ACCOUNT
	// ===================================================================

	private static final String UPDATE_ORPHAN_TRANSACTIONS_1 = "UPDATE "+TRANSACTION_TABLE+" SET "+
								TransactionColumns.TO_ACCOUNT_ID+"=0, "+
								TransactionColumns.TO_AMOUNT+"=0 "+
								"WHERE "+TransactionColumns.TO_ACCOUNT_ID+"=?";
	private static final String UPDATE_ORPHAN_TRANSACTIONS_2 = "UPDATE "+TRANSACTION_TABLE+" SET "+
								TransactionColumns.FROM_ACCOUNT_ID+"="+TransactionColumns.TO_ACCOUNT_ID+", "+
								TransactionColumns.FROM_AMOUNT+"="+TransactionColumns.TO_AMOUNT+", "+
								TransactionColumns.TO_ACCOUNT_ID+"=0, "+
								TransactionColumns.TO_AMOUNT+"=0 "+
								"WHERE "+TransactionColumns.FROM_ACCOUNT_ID+"=? AND "+
										 TransactionColumns.TO_ACCOUNT_ID+">0";
	
	public int deleteAccount(long id) {
		db.beginTransaction();
		try {
			String[] sid = new String[]{String.valueOf(id)};
			db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_1, sid);
			db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_2, sid);
			db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID
					+" in (SELECT _id from "+TRANSACTION_TABLE+" where "+TransactionColumns.FROM_ACCOUNT_ID+"=?)", sid);
			db.delete(TRANSACTION_TABLE, TransactionColumns.FROM_ACCOUNT_ID+"=?", sid);
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
				TransactionColumns.ID+"=?", new String[]{String.valueOf(id)}, 
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
				BlotterColumns.DATETIME+" DESC");
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
				BlotterColumns.BALANCE_GROUPBY, null, null);
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
		return duplicateTransaction(id, 0);
	}
	
	public long duplicateTransaction(long id, int isTemplate) {
		Transaction transaction = getTransaction(id);
		transaction.id = -1;
		transaction.isTemplate = isTemplate;
		transaction.dateTime = System.currentTimeMillis();
		if (isTemplate == 0) {
			transaction.recurrence = null;
			transaction.notificationOptions = null;
		}
		HashMap<Long, String> attributesMap = getAllAttributesForTransaction(id);
		LinkedList<TransactionAttribute> attributes = new LinkedList<TransactionAttribute>();
		for (long attributeId : attributesMap.keySet()) {
			TransactionAttribute ta = new TransactionAttribute();
			ta.attributeId = attributeId;
			ta.value = attributesMap.get(attributeId);
			attributes.add(ta);
		}
		return insertOrUpdate(transaction, attributes);
	}

	public long insertOrUpdate(Transaction transaction, LinkedList<TransactionAttribute> attributes) {
		db.beginTransaction();
		try {
			long transactionId;
			if (transaction.id == -1) {
				transactionId = insertTransaction(transaction);
			} else {
				updateTransaction(transaction);
				transactionId = transaction.id;
			}
			db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID+"=?", 
					new String[]{String.valueOf(transactionId)});
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
		db.update(TRANSACTION_TABLE, t.toValues(), TransactionColumns.ID+"=?", 
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
			db.delete(TRANSACTION_TABLE, TransactionColumns.ID+"=?", sid);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();			
		}
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
			return insertChildCategory(parentId, categoryTitle);
		} else {
			long mateId = -1;
			for (Category c : subordinates) {
				if (categoryTitle.compareTo(c.title) <= 0) {
					break;
				}
				mateId = c.id;
			}
			if (mateId == -1) {
				return insertChildCategory(parentId, categoryTitle);
			} else {
				return insertMateCategory(mateId, categoryTitle);
			}
		}
	}

	private long updateCategory(Category category) {
		Category oldCategory = getCategory(category.id);
		if (oldCategory.getParentId() == category.getParentId()) {
			updateCategory(category.id, category.title);
		} else {
			moveCategory(category.id, category.getParentId(), category.title);
		}
		return category.id;
	}

	private static final String GET_PARENT_SQL = "(SELECT "
		+ "parent."+CategoryColumns.ID+" AS "+CategoryColumns.ID
		+ " FROM "
		+ CATEGORY_TABLE+" AS node"+","
		+ CATEGORY_TABLE+" AS parent "
		+" WHERE "
		+" node."+CategoryColumns.LEFT+" BETWEEN parent."+CategoryColumns.LEFT+" AND parent."+CategoryColumns.RIGHT
		+" AND node."+CategoryColumns.ID+"=?"
		+" AND parent."+CategoryColumns.ID+"!=?"
		+" ORDER BY parent."+CategoryColumns.LEFT+" DESC)";
	
	public Category getCategory(long id) {
		Cursor c = db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, 
				CategoryViewColumns.ID+"=?", new String[]{String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToNext()) {				
				Category cat = new Category();
				cat.id = id;
				cat.title = c.getString(CategoryViewColumns.Indicies.TITLE);
				cat.level = c.getInt(CategoryViewColumns.Indicies.LEVEL);
				cat.left = c.getInt(CategoryViewColumns.Indicies.LEFT);
				cat.right = c.getInt(CategoryViewColumns.Indicies.RIGHT);
				String s = String.valueOf(id); 
				Cursor c2 = db.query(GET_PARENT_SQL, new String[]{CategoryColumns.ID}, null, new String[]{s,s}, 
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
				CategoryViewColumns.LEFT+"=?", new String[]{String.valueOf(left)}, null, null, null);
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

	public ArrayList<Category> getAllCategoriesTree(boolean includeNoCategory) {
		Cursor c = getAllCategories(includeNoCategory);
		try { 
			ArrayList<Category> list = new CategoryTree<Category>(){
				@Override
				protected Category createNode(Cursor c) {
					return Category.formCursor(c);
				}
			}.create(c);
			return list;
		} finally {
			c.close();
		}
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
				includeNoCategory ? null : CategoryViewColumns.ID+"!=0", null, null, null, null);
	}
	
	public Cursor getAllCategoriesWithoutSubtree(long id) {
		long left = 0, right = 0;
		Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.LEFT, CategoryColumns.RIGHT}, 
				CategoryColumns.ID+"=?", new String[]{String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToFirst()) {
				left = c.getLong(0);
				right = c.getLong(1);
			}
		} finally {
			c.close();
		}
		return db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION, 
				"NOT ("+CategoryViewColumns.LEFT+">="+left+" AND "+CategoryColumns.RIGHT+"<="+right+")", null, null, null, null);
	}

	private static final String INSERT_CATEGORY_UPDATE_RIGHT = "UPDATE "+CATEGORY_TABLE+" SET "+CategoryColumns.RIGHT+"="+CategoryColumns.RIGHT+"+2 WHERE "+CategoryColumns.RIGHT+">?";
	private static final String INSERT_CATEGORY_UPDATE_LEFT = "UPDATE "+CATEGORY_TABLE+" SET "+CategoryColumns.LEFT+"="+CategoryColumns.LEFT+"+2 WHERE "+CategoryColumns.LEFT+">?";
	
	public long insertChildCategory(long parentId, String title) {
		//DECLARE v_leftkey INT UNSIGNED DEFAULT 0;
		//SELECT l INTO v_leftkey FROM `nset` WHERE `id` = ParentID;
		//UPDATE `nset` SET `r` = `r` + 2 WHERE `r` > v_leftkey;
		//UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_leftkey;
		//INSERT INTO `nset` (`name`, `l`, `r`) VALUES (NodeName, v_leftkey + 1, v_leftkey + 2);
		return insertCategory(CategoryColumns.LEFT, parentId, title);
	}

	public long insertMateCategory(long categoryId, String title) {
		//DECLARE v_rightkey INT UNSIGNED DEFAULT 0;
		//SELECT `r` INTO v_rightkey FROM `nset` WHERE `id` = MateID;
		//UPDATE `	nset` SET `r` = `r` + 2 WHERE `r` > v_rightkey;
		//UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_rightkey;
		//INSERT `nset` (`name`, `l`, `r`) VALUES (NodeName, v_rightkey + 1, v_rightkey + 2);
		return insertCategory(CategoryColumns.RIGHT, categoryId, title);
	}

	private long insertCategory(String field, long categoryId, String title) {
		int num = 0;
		Cursor c = db.query(CATEGORY_TABLE, new String[]{field}, 
				CategoryColumns.ID+"=?", new String[]{String.valueOf(categoryId)}, null, null, null);
		try {
			if (c.moveToFirst()) {
				num = c.getInt(0);
			}
		} finally  {
			c.close();
		}
		db.beginTransaction();
		try {
			String[] args = new String[]{String.valueOf(num)};
			db.execSQL(INSERT_CATEGORY_UPDATE_RIGHT, args);
			db.execSQL(INSERT_CATEGORY_UPDATE_LEFT, args);
			ContentValues values = new ContentValues();
			values.put(CategoryColumns.TITLE, title);
			values.put(CategoryColumns.LEFT, num+1);
			values.put(CategoryColumns.RIGHT, num+2);
			long id = db.insert(CATEGORY_TABLE, null, values);
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
	}

	private static final String V_SUBORDINATES = "(SELECT " 
	+"node."+CategoryColumns.ID+" as "+CategoryViewColumns.ID+", "
	+"node."+CategoryColumns.TITLE+" as "+CategoryViewColumns.TITLE+", "
	+"(COUNT(parent."+CategoryColumns.ID+") - (sub_tree.depth + 1)) AS "+CategoryViewColumns.LEVEL
	+" FROM "
	+CATEGORY_TABLE+" AS node, "
	+CATEGORY_TABLE+" AS parent, "
	+CATEGORY_TABLE+" AS sub_parent, "
	+"("
		+"SELECT node."+CategoryColumns.ID+" as "+CategoryColumns.ID+", "
		+"(COUNT(parent."+CategoryColumns.ID+") - 1) AS depth"
		+" FROM "
		+CATEGORY_TABLE+" AS node, "
		+CATEGORY_TABLE+" AS parent "
		+" WHERE node."+CategoryColumns.LEFT+" BETWEEN parent."+CategoryColumns.LEFT+" AND parent."+CategoryColumns.RIGHT
		+" AND node."+CategoryColumns.ID+"=?"
		+" GROUP BY node."+CategoryColumns.ID
		+" ORDER BY node."+CategoryColumns.LEFT
	+") AS sub_tree "
	+" WHERE node."+CategoryColumns.LEFT+" BETWEEN parent."+CategoryColumns.LEFT+" AND parent."+CategoryColumns.RIGHT
	+" AND node."+CategoryColumns.LEFT+" BETWEEN sub_parent."+CategoryColumns.LEFT+" AND sub_parent."+CategoryColumns.RIGHT
	+" AND sub_parent."+CategoryColumns.ID+" = sub_tree."+CategoryColumns.ID
	+" GROUP BY node."+CategoryColumns.ID
	+" HAVING "+CategoryViewColumns.LEVEL+"=1"
	+" ORDER BY node."+CategoryColumns.LEFT
	+")";
	
	public List<Category> getSubordinates(long parentId) {
		List<Category> list = new LinkedList<Category>();
		Cursor c = db.query(V_SUBORDINATES, new String[]{CategoryViewColumns.ID, CategoryViewColumns.TITLE, CategoryViewColumns.LEVEL}, null, 
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
		+" SET "+TransactionColumns.CATEGORY_ID+"=0 WHERE "
		+TransactionColumns.CATEGORY_ID+" IN ("
		+"SELECT "+CategoryColumns.ID+" FROM "+CATEGORY_TABLE+" WHERE "
		+CategoryColumns.LEFT+" BETWEEN ? AND ?)";
	private static final String DELETE_CATEGORY_UPDATE2 = "UPDATE "+CATEGORY_TABLE
		+" SET "+CategoryColumns.LEFT+"=(CASE WHEN "+CategoryColumns.LEFT+">%s THEN "
		+CategoryColumns.LEFT+"-%s ELSE "+CategoryColumns.LEFT+" END),"
		+CategoryColumns.RIGHT+"="+CategoryColumns.RIGHT+"-%s"
		+" WHERE "+CategoryColumns.RIGHT+">%s";

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
		Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.LEFT, CategoryColumns.RIGHT}, 
				CategoryColumns.ID+"=?", new String[]{String.valueOf(categoryId)}, null, null, null);
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
			db.delete(CATEGORY_TABLE, CategoryColumns.LEFT+" BETWEEN ? AND ?", args);
			db.execSQL(String.format(DELETE_CATEGORY_UPDATE2, left, width, width, right));
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	private void updateCategory(long id, String title) {
		ContentValues values = new ContentValues();
		values.put(CategoryColumns.TITLE, title);
		db.update(CATEGORY_TABLE, values, CategoryColumns.ID+"=?", new String[]{String.valueOf(id)});
	}
	
	public void moveCategory(long id, long newParentId, String title) {
		db.beginTransaction();
		try {
			
			updateCategory(id, title);
			
			long origin_lft, origin_rgt, new_parent_rgt;
			Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.LEFT, CategoryColumns.RIGHT}, 
					CategoryColumns.ID+"=?", new String[]{String.valueOf(id)}, null, null, null);
			try {
				if (c.moveToFirst()) {
					origin_lft = c.getLong(0);
					origin_rgt = c.getLong(1);
				} else {
					return;
				}
			} finally {
				c.close();
			}
			c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.RIGHT}, 
					CategoryColumns.ID+"=?", new String[]{String.valueOf(newParentId)}, null, null, null);
			try {
				if (c.moveToFirst()) {
					new_parent_rgt = c.getLong(0);
				} else {
					return;
				}
			} finally {
				c.close();
			}
		
			db.execSQL("UPDATE "+CATEGORY_TABLE+" SET "
				+CategoryColumns.LEFT+" = "+CategoryColumns.LEFT+" + CASE "
				+" WHEN "+new_parent_rgt+" < "+origin_lft
				+" THEN CASE "
				+" WHEN "+CategoryColumns.LEFT+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+new_parent_rgt+" - "+origin_lft
				+" WHEN "+CategoryColumns.LEFT+" BETWEEN "+new_parent_rgt+" AND "+(origin_lft-1)
				+" THEN "+(origin_rgt-origin_lft+1)
				+" ELSE 0 END "
				+" WHEN "+new_parent_rgt+" > "+origin_rgt
				+" THEN CASE "
				+" WHEN "+CategoryColumns.LEFT+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+(new_parent_rgt-origin_rgt-1)
				+" WHEN "+CategoryColumns.LEFT+" BETWEEN "+(origin_rgt+1)+" AND "+(new_parent_rgt-1)
				+" THEN "+(origin_lft - origin_rgt - 1)
				+" ELSE 0 END "
				+" ELSE 0 END,"
				+CategoryColumns.RIGHT+" = "+CategoryColumns.RIGHT+" + CASE "
				+" WHEN "+new_parent_rgt+" < "+origin_lft
				+" THEN CASE "
				+" WHEN "+CategoryColumns.RIGHT+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+(new_parent_rgt-origin_lft)
				+" WHEN "+CategoryColumns.RIGHT+" BETWEEN "+new_parent_rgt+" AND "+(origin_lft - 1)
				+" THEN "+(origin_rgt-origin_lft+1)
				+" ELSE 0 END "
				+" WHEN "+new_parent_rgt+" > "+origin_rgt
				+" THEN CASE "
				+" WHEN "+CategoryColumns.RIGHT+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+(new_parent_rgt-origin_rgt-1)
				+" WHEN "+CategoryColumns.RIGHT+" BETWEEN "+(origin_rgt+1)+" AND "+(new_parent_rgt-1)
				+" THEN "+(origin_lft-origin_rgt-1)
				+" ELSE 0 END "
				+" ELSE 0 END");
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
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
}

