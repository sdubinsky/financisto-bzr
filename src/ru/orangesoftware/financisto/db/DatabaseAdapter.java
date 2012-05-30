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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.CategoryTree.NodeCreator;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.rates.*;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.Utils;

import java.math.BigDecimal;
import java.util.*;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;

public class DatabaseAdapter {

	private final Context context;
    private final DatabaseHelper dbHelper;
	private final MyEntityManager em;

    private boolean updateAccountBalance = true;

    public DatabaseAdapter(Context context) {
        this(context, DatabaseHelper.getHelper(context));
    }

	public DatabaseAdapter(Context context, DatabaseHelper dbHelper) {
		this.context = context;
        this.dbHelper = dbHelper;
        this.em = new MyEntityManager(context, dbHelper);
	}
	
	public void open() {
	}

	public void close() {
	}
	
	public SQLiteDatabase db() {
		return dbHelper.getWritableDatabase();
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
        SQLiteDatabase db = db();
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
        Transaction t = em.get(Transaction.class, id);
        if (t != null) {
            t.systemAttributes = getSystemAttributesForTransaction(id);
            if (t.isSplitParent()) {
                t.splits = em.getSplitsForTransaction(t.id);
            }
            return t;
        }
		return new Transaction();
	}

    public Cursor getBlotter(WhereFilter filter) {
        return getBlotter(V_BLOTTER, filter);
    }

    public Cursor getBlotterForAccount(WhereFilter filter) {
        WhereFilter accountFilter = enhanceFilterForAccountBlotter(filter);
        return getBlotter(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, accountFilter);
    }

    public static WhereFilter enhanceFilterForAccountBlotter(WhereFilter filter) {
        WhereFilter accountFilter = WhereFilter.copyOf(filter);
        accountFilter.put(WhereFilter.Criteria.raw(BlotterColumns.parent_id+"=0 OR "+ BlotterColumns.is_transfer+"=-1"));
        return accountFilter;
    }

    public Cursor getBlotterForAccountWithSplits(WhereFilter filter) {
        return getBlotter(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, filter);
    }

	private Cursor getBlotter(String view, WhereFilter filter) {
        long t0 = System.currentTimeMillis();
        try {
            String sortOrder = getBlotterSortOrder(filter);
            return db().query(view, BlotterColumns.NORMAL_PROJECTION,
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

    public Cursor getAllScheduledTransactions() {
        return db().query(V_ALL_TRANSACTIONS, BlotterColumns.NORMAL_PROJECTION,
                BlotterColumns.is_template+"=? AND "+BlotterColumns.parent_id+"=?", new String[]{"2","0"},
                null, null, BlotterFilter.SORT_OLDER_TO_NEWER);
    }

	public Cursor getAllTemplates(WhereFilter filter) {
		long t0 = System.currentTimeMillis();
		try {
			return db().query(V_ALL_TRANSACTIONS, BlotterColumns.NORMAL_PROJECTION,
				filter.getSelection(), filter.getSelectionArgs(), null, null, 
				BlotterFilter.SORT_NEWER_TO_OLDER);
		} finally {
			long t1 = System.currentTimeMillis();
			Log.i("DB", "getBlotter "+(t1-t0)+"ms");
		}
	}

    public Cursor getBlotterWithSplits(String where) {
        return db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, BlotterColumns.NORMAL_PROJECTION, where, null, null, null,
                BlotterColumns.datetime+" DESC");
    }

	private static final String LOCATION_COUNT_UPDATE = "UPDATE "+LOCATIONS_TABLE
	+" SET count=count+(?) WHERE _id=?";

	private void updateLocationCount(long locationId, int count) {
		db().execSQL(LOCATION_COUNT_UPDATE, new Object[]{count, locationId});
	}

    private static final String ACCOUNT_LAST_CATEGORY_UPDATE = "UPDATE " + ACCOUNT_TABLE
            + " SET " + AccountColumns.LAST_CATEGORY_ID + "=? "
            + " WHERE " + AccountColumns.ID + "=?";

    private static final String ACCOUNT_LAST_ACCOUNT_UPDATE = "UPDATE "+ACCOUNT_TABLE
	+" SET "+AccountColumns.LAST_ACCOUNT_ID+"=? "
	+" WHERE "+AccountColumns.ID+"=?";

    private static final String PAYEE_LAST_CATEGORY_UPDATE = "UPDATE "+PAYEE_TABLE
    +" SET last_category_id=(?) WHERE _id=?";

	private static final String CATEGORY_LAST_LOCATION_UPDATE = "UPDATE "+CATEGORY_TABLE
	+" SET last_location_id=(?) WHERE _id=?";

	private static final String CATEGORY_LAST_PROJECT_UPDATE = "UPDATE "+CATEGORY_TABLE
	+" SET last_project_id=(?) WHERE _id=?";

	private void updateLastUsed(Transaction t) {
        SQLiteDatabase db = db();
		if (t.isTransfer()) {
			db.execSQL(ACCOUNT_LAST_ACCOUNT_UPDATE, new Object[]{t.toAccountId, t.fromAccountId});
		}
        db.execSQL(ACCOUNT_LAST_CATEGORY_UPDATE, new Object[]{t.categoryId, t.fromAccountId});
		db.execSQL(PAYEE_LAST_CATEGORY_UPDATE, new Object[]{t.categoryId, t.payeeId});
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
        SQLiteDatabase db = db();
		db.beginTransaction();
		try {
			long now = System.currentTimeMillis();
			Transaction transaction = getTransaction(id);
            if (transaction.isSplitChild()) {
                id = transaction.parentId;
                transaction = getTransaction(id);
            }
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
			long transactionId = insertTransaction(transaction);
            HashMap<Long, String> attributesMap = getAllAttributesForTransaction(id);
            LinkedList<TransactionAttribute> attributes = new LinkedList<TransactionAttribute>();
            for (long attributeId : attributesMap.keySet()) {
                TransactionAttribute ta = new TransactionAttribute();
                ta.attributeId = attributeId;
                ta.value = attributesMap.get(attributeId);
                attributes.add(ta);
            }
			if (attributes.size() > 0) {
				insertAttributes(transactionId, attributes);
			}
            List<Transaction> splits = em.getSplitsForTransaction(id);
            if (multiplier > 1) {
                for (Transaction split : splits) {
                    split.fromAmount *= multiplier;
                }
            }
            transaction.id = transactionId;
            transaction.splits = splits;
            insertSplits(transaction);
			db.setTransactionSuccessful();
			return transactionId;
		} finally {
			db.endTransaction();
		}
	}

    public long insertOrUpdate(Transaction transaction) {
        return insertOrUpdate(transaction, Collections.<TransactionAttribute>emptyList());
    }

	public long insertOrUpdate(Transaction transaction, List<TransactionAttribute> attributes) {
        SQLiteDatabase db = db();
		db.beginTransaction();
		try {
            long id = insertOrUpdateInTransaction(transaction, attributes);
            db.setTransactionSuccessful();
            return id;
		} finally {
			db.endTransaction();
		}
	}

    public long insertOrUpdateInTransaction(Transaction transaction, List<TransactionAttribute> attributes) {
        long transactionId;
        transaction.lastRecurrence = System.currentTimeMillis();
        if (transaction.id == -1) {
            transactionId = insertTransaction(transaction);
        } else {
            updateTransaction(transaction);
            transactionId = transaction.id;
            db().delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID+"=?",
                    new String[]{String.valueOf(transactionId)});
            deleteSplitsForParentTransaction(transactionId);
        }
        if (attributes != null) {
            insertAttributes(transactionId, attributes);
        }
        transaction.id = transactionId;
        insertSplits(transaction);
        return transactionId;
    }

    public void insertWithoutUpdatingBalance(Transaction transaction) {
        updateAccountBalance = false;
        try {
            transaction.id = insertTransaction(transaction);
            insertSplits(transaction);
        } finally {
            updateAccountBalance = true;
        }
    }

    private void insertAttributes(long transactionId, List<TransactionAttribute> attributes) {
		for (TransactionAttribute a : attributes) {
			a.transactionId = transactionId;
			ContentValues values = a.toValues();
			db().insert(TRANSACTION_ATTRIBUTE_TABLE, null, values);
		}
	}

    private void insertSplits(Transaction parent) {
        List<Transaction> splits = parent.splits;
        if (splits != null) {
            for (Transaction split : splits) {
                split.id = -1;
                split.parentId = parent.id;
                split.dateTime = parent.dateTime;
                split.fromAccountId = parent.fromAccountId;
                split.payeeId = parent.payeeId;
                split.isTemplate = parent.isTemplate;
                split.status = parent.status;
                insertTransaction(split);
            }
        }
    }

    public long insertPayee(String payee) {
        if (Utils.isEmpty(payee)) {
            return 0;
        } else {
            Payee p = em.insertPayee(payee);
            return p.id;
        }
    }

    private long insertTransaction(Transaction t) {
        long id = db().insert(TRANSACTION_TABLE, null, t.toValues());
        if (updateAccountBalance) {
            if (!t.isTemplateLike()) {
                if (t.isSplitChild()) {
                    if (t.isTransfer()) {
                        updateToAccountBalance(t, id);
                    }
                } else {
                    updateFromAccountBalance(t, id);
                    updateToAccountBalance(t, id);
                    updateLocationCount(t.locationId, 1);
                    updateLastUsed(t);
                }
            }
        }
        return id;
    }

    private void updateFromAccountBalance(Transaction t, long id) {
        updateAccountBalance(t.fromAccountId, t.fromAmount);
        insertRunningBalance(t.fromAccountId, id, t.dateTime, t.fromAmount, t.fromAmount);
    }

    private void updateToAccountBalance(Transaction t, long id) {
        updateAccountBalance(t.toAccountId, t.toAmount);
        insertRunningBalance(t.toAccountId, id, t.dateTime, t.toAmount, t.toAmount);
    }

    private void updateTransaction(Transaction t) {
		if (t.isNotTemplateLike()) {
			Transaction oldT = getTransaction(t.id);
			updateAccountBalance(oldT.fromAccountId, oldT.fromAmount, t.fromAccountId, t.fromAmount);
			updateAccountBalance(oldT.toAccountId, oldT.toAmount, t.toAccountId, t.toAmount);
            updateRunningBalance(oldT, t);
			if (oldT.locationId != t.locationId) {
				updateLocationCount(oldT.locationId, -1);
				updateLocationCount(t.locationId, 1);
			}
		}
		db().update(TRANSACTION_TABLE, t.toValues(), TransactionColumns._id +"=?",
				new String[]{String.valueOf(t.id)});		
	}

    public void updateTransactionStatus(long id, TransactionStatus status) {
        Transaction t = getTransaction(id);
        t.status = status;
        updateTransaction(t);
    }

    public void deleteTransaction(long id) {
        SQLiteDatabase db = db();
		db.beginTransaction();
		try {
            deleteTransactionNoDbTransaction(id);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();			
		}
	}
	
	public void deleteTransactionNoDbTransaction(long id) {
        Transaction t = getTransaction(id);
        if (t.isNotTemplateLike()) {
            revertFromAccountBalance(t);
            revertToAccountBalance(t);
            updateLocationCount(t.locationId, -1);
        }
        String[] sid = new String[]{String.valueOf(id)};
        SQLiteDatabase db = db();
        db.delete(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.TRANSACTION_ID+"=?", sid);
        db.delete(TRANSACTION_TABLE, TransactionColumns._id+"=?", sid);
        deleteSplitsForParentTransaction(id);
	}

    private void deleteSplitsForParentTransaction(long parentId) {
        List<Transaction> splits = em().getSplitsForTransaction(parentId);
        for (Transaction split : splits) {
            if (split.isTransfer()) {
                revertToAccountBalance(split);
            }
        }
        db().delete(TRANSACTION_TABLE, TransactionColumns.parent_id+"=?", new String[]{String.valueOf(parentId)});
    }

    private void revertFromAccountBalance(Transaction t) {
        updateAccountBalance(t.fromAccountId, -t.fromAmount);
        deleteRunningBalance(t.fromAccountId, t.id, t.fromAmount, t.dateTime);
    }

    private void revertToAccountBalance(Transaction t) {
        updateAccountBalance(t.toAccountId, -t.toAmount);
        deleteRunningBalance(t.toAccountId, t.id, t.toAmount, t.dateTime);
    }

    private void updateAccountBalance(long oldAccountId, long oldAmount, long newAccountId, long newAmount) {
        if (oldAccountId == newAccountId) {
            updateAccountBalance(newAccountId, newAmount - oldAmount);
        } else {
            updateAccountBalance(oldAccountId, -oldAmount);
            updateAccountBalance(newAccountId, newAmount);
        }
    }

    private static final String ACCOUNT_TOTAL_AMOUNT_UPDATE = "UPDATE "+ACCOUNT_TABLE
	+" SET "+AccountColumns.TOTAL_AMOUNT+"="+AccountColumns.TOTAL_AMOUNT+"+(?) "
	+" WHERE "+AccountColumns.ID+"=?";

    private void updateAccountBalance(long accountId, long deltaAmount) {
        if (accountId <= 0) {
            return;
        }
        db().execSQL(ACCOUNT_TOTAL_AMOUNT_UPDATE, new Object[]{deltaAmount, accountId});
    }

    private static final String INSERT_RUNNING_BALANCE =
            "insert or replace into running_balance(account_id,transaction_id,datetime,balance) values (?,?,?,?)";

    private static final String UPDATE_RUNNING_BALANCE =
            "update running_balance set balance = balance+(?) where account_id = ? and datetime > ?";

    private static final String DELETE_RUNNING_BALANCE =
            "delete from running_balance where account_id = ? and transaction_id = ?";

    private void insertRunningBalance(long accountId, long transactionId, long datetime, long amount, long deltaAmount) {
        if (accountId <= 0) {
            return;
        }
        long previousTransactionBalance = fetchAccountBalanceAtTheTime(accountId, datetime);
        SQLiteDatabase db = db();
        db.execSQL(INSERT_RUNNING_BALANCE, new Object[]{accountId, transactionId, datetime, previousTransactionBalance+amount});
        db.execSQL(UPDATE_RUNNING_BALANCE, new Object[]{deltaAmount, accountId, datetime});
    }

    private void updateRunningBalance(Transaction oldTransaction, Transaction newTransaction) {
        deleteRunningBalance(oldTransaction.fromAccountId, oldTransaction.id, oldTransaction.fromAmount, oldTransaction.dateTime);
        insertRunningBalance(newTransaction.fromAccountId, newTransaction.id, newTransaction.dateTime,
                newTransaction.fromAmount, newTransaction.fromAmount);
        deleteRunningBalance(oldTransaction.toAccountId, oldTransaction.id, oldTransaction.toAmount, oldTransaction.dateTime);
        insertRunningBalance(newTransaction.toAccountId, newTransaction.id, newTransaction.dateTime,
                newTransaction.toAmount, newTransaction.toAmount);
    }

    private void deleteRunningBalance(long accountId, long transactionId, long amount, long dateTime) {
        if (accountId <= 0) {
            return;
        }
        SQLiteDatabase db = db();
        db.execSQL(DELETE_RUNNING_BALANCE, new Object[]{accountId, transactionId});
        db.execSQL(UPDATE_RUNNING_BALANCE, new Object[]{-amount, accountId, dateTime});
    }

    private long fetchAccountBalanceAtTheTime(long accountId, long datetime) {
        Cursor c = db().rawQuery("select balance from running_balance where account_id = ? and datetime <= ? order by datetime desc, transaction_id desc limit 1",
                new String[]{String.valueOf(accountId), String.valueOf(datetime)});
        try {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    // ===================================================================
	// CATEGORY
	// ===================================================================

	public long insertOrUpdate(Category category, List<Attribute> attributes) {
        SQLiteDatabase db = db();
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
            category.id = id;
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
	}
	
	private void addAttributes(long categoryId, List<Attribute> attributes) {
        SQLiteDatabase db = db();
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
        SQLiteDatabase db = db();
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

	public Category getCategoryByTitle(String title) {
        SQLiteDatabase db = db();
		Cursor c = db.query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION,
				CategoryViewColumns.title+"=?", new String[]{String.valueOf(title)}, null, null, null);
		try {
			if (c.moveToNext()) {				
				Category cat = new Category();
				cat.id = c.getInt(CategoryViewColumns._id.ordinal());
				cat.title = title;
				cat.level = c.getInt(CategoryViewColumns.level.ordinal());
				cat.left = c.getInt(CategoryViewColumns.left.ordinal());
				cat.right = c.getInt(CategoryViewColumns.right.ordinal());
                cat.type = c.getInt(CategoryViewColumns.type.ordinal());
				String s = String.valueOf(cat.id);
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
				return null;
			}
		} finally {
			c.close();
		}
	}
	
	public Category getCategoryByLeft(long left) {
        SQLiteDatabase db = db();
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

	public CategoryTree<Category> getCategoriesTree(boolean includeNoCategory) {
		Cursor c = getCategories(includeNoCategory);
		try {
            return CategoryTree.createFromCursor(c, new NodeCreator<Category>(){
                @Override
                public Category createNode(Cursor c) {
                    return Category.formCursor(c);
                }
            });
		} finally {
			c.close();
		}
	}
	
	public Map<Long, Category> getCategoriesMap(boolean includeNoCategory) {
		return getCategoriesTree(includeNoCategory).asMap();
	}

	public List<Category> getCategoriesList(boolean includeNoCategory) {
		Cursor c = getCategories(includeNoCategory);
        return categoriesAsList(c);
	}

    public Cursor getAllCategories() {
        return db().query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION,
                null, null, null, null, null);
    }

    public List<Category> getAllCategoriesList() {
        Cursor c = getAllCategories();
        return categoriesAsList(c);
    }

    private List<Category> categoriesAsList(Cursor c) {
        ArrayList<Category> list = new ArrayList<Category>();
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

	public Cursor getCategories(boolean includeNoCategory) {
		return db().query(V_CATEGORY, CategoryViewColumns.NORMAL_PROJECTION,
				includeNoCategory ? CategoryViewColumns._id+">=0" : CategoryViewColumns._id+">0", null, null, null, null);
	}

	public Cursor getCategoriesWithoutSubtree(long id) {
        SQLiteDatabase db = db();
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
				"(NOT ("+CategoryViewColumns.left+">=? AND "+CategoryColumns.right+"<=?)) AND "+CategoryViewColumns._id+">=0",
                new String[]{String.valueOf(left), String.valueOf(right)}, null, null, null);
	}

    public List<Category> getCategoriesWithoutSubtreeAsList(long categoryId) {
        List<Category> list = new ArrayList<Category>();
        Cursor c = getCategoriesWithoutSubtree(categoryId);
        try {
            while(c.moveToNext()) {
                Category category = Category.formCursor(c);
                list.add(category);
            }
            return list;
        } finally {
            c.close();
        }
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
        SQLiteDatabase db = db();
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
        db().execSQL(CATEGORY_UPDATE_CHILDREN_TYPES, new Object[]{type, left, right});
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
		Cursor c = db().query(V_SUBORDINATES, new String[]{CategoryViewColumns._id.name(), CategoryViewColumns.title.name(), CategoryViewColumns.level.name()}, null,
				new String[]{String.valueOf(parentId)}, null, null, null);
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
        SQLiteDatabase db = db();
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
		db().update(CATEGORY_TABLE, values, CategoryColumns._id+"=?", new String[]{String.valueOf(id)});
	}
	
    public void insertCategoryTreeInTransaction(CategoryTree<Category> tree) {
        db().delete("category", "_id > 0", null);
        insertCategoryInTransaction(tree);
        updateCategoryTreeInTransaction(tree);
    }

    private void insertCategoryInTransaction(CategoryTree<Category> tree) {
        for (Category category : tree) {
            em.reInsertCategory(category);
            if (category.hasChildren()) {
                insertCategoryInTransaction(category.children);
            }
        }
    }

    public void updateCategoryTree(CategoryTree<Category> tree) {
        SQLiteDatabase db = db();
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
			db().update(CATEGORY_TABLE, values, WHERE_CATEGORY_ID, sid);
			if (c.hasChildren()) {
				updateCategoryTreeInTransaction(c.children);
			}
		}
	}

	public void moveCategory(long id, long newParentId, String title, int type) {
        updateCategory(id, title, type);

        SQLiteDatabase db = db();
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
        SQLiteDatabase db = db();
        db.execSQL(UPDATE_CATEGORY_TYPE, new Object[]{type, id});
    }

    // ===================================================================
	// ATTRIBUTES
	// ===================================================================

	public ArrayList<Attribute> getAttributesForCategory(long categoryId) {
		Cursor c = db().query(V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION,
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
		Cursor c = db().query(V_ATTRIBUTES, AttributeColumns.NORMAL_PROJECTION,
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
		Cursor c = db().query(ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION,
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
        SQLiteDatabase db = db();
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
		return db().insert(ATTRIBUTES_TABLE, null, values);
	}

	private void updateAttribute(Attribute attribute) {
		ContentValues values = attribute.toValues();
		db().update(ATTRIBUTES_TABLE, values, AttributeColumns.ID+"=?", new String[]{String.valueOf(attribute.id)});
	}

	public Cursor getAllAttributes() {
		return db().query(ATTRIBUTES_TABLE, AttributeColumns.NORMAL_PROJECTION,
				AttributeColumns.ID+">0", null, null, null, AttributeColumns.NAME);
	}

	public HashMap<Long, String> getAllAttributesMap() {
		Cursor c = db().query(V_ATTRIBUTES, AttributeViewColumns.NORMAL_PROJECTION, null, null, null, null,
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
		Cursor c = db().query(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION,
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
		Cursor c = db().query(TRANSACTION_ATTRIBUTE_TABLE, TransactionAttributeColumns.NORMAL_PROJECTION,
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
		Cursor c = db().query(LOCATIONS_TABLE, new String[]{LocationColumns.NAME},
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

    /**
     * Sets status=CL (Cleared) for the selected transactions
     * @param ids selected transactions' ids
     */
	public void clearSelectedTransactions(long[] ids) {
		String sql = "UPDATE "+TRANSACTION_TABLE+" SET "+TransactionColumns.status +"='"+TransactionStatus.CL+"'";
		runInTransaction(sql, ids);
	}

    /**
     * Sets status=RC (Reconciled) for the selected transactions
     * @param ids selected transactions' ids
     */
	public void reconcileSelectedTransactions(long[] ids) {
		String sql = "UPDATE "+TRANSACTION_TABLE+" SET "+TransactionColumns.status +"='"+TransactionStatus.RC+"'";
		runInTransaction(sql, ids);
	}

    /**
     * Deletes the selected transactions
     * @param ids selected transactions' ids
     */
	public void deleteSelectedTransactions(long[] ids) {
        SQLiteDatabase db = db();
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
        SQLiteDatabase db = db();
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
                                .append(TransactionColumns.parent_id)
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

	public long[] storeMissedSchedules(List<RestoredTransaction> restored, long now) {
        SQLiteDatabase db = db();
		db.beginTransaction();
		try {
            int count = restored.size();
            long[] restoredIds = new long[count];
			HashMap<Long, Transaction> transactions = new HashMap<Long, Transaction>();
			for (int i=0; i<count; i++) {
                RestoredTransaction rt = restored.get(i);
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
				restoredIds[i] = insertOrUpdate(t);
				t.id = transactionId;
			}
			for (Transaction t : transactions.values()) {
				db.execSQL(UPDATE_LAST_RECURRENCE, new Object[]{now, t.id});		
			}
			db.setTransactionSuccessful();
            return restoredIds;
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
		
		Cursor c = db().query(CCARD_CLOSING_DATE_TABLE, new String[] {CreditCardClosingDateColumns.CLOSING_DAY},
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
	

	public void setCustomClosingDay(long accountId, int period, int closingDay) {
		ContentValues values = new ContentValues();
        values.put(CreditCardClosingDateColumns.ACCOUNT_ID, Long.toString(accountId));
        values.put(CreditCardClosingDateColumns.PERIOD, Integer.toString(period));
        values.put(CreditCardClosingDateColumns.CLOSING_DAY, Integer.toString(closingDay));
		db().insert(CCARD_CLOSING_DATE_TABLE, null, values);
	}
	
	public void deleteCustomClosingDay(long accountId, int period) {
		String where = CreditCardClosingDateColumns.ACCOUNT_ID+"=? AND "+
		   			   CreditCardClosingDateColumns.PERIOD+"=?";
		String[] args = new String[] {Long.toString(accountId), Integer.toString(period)};
		db().delete(CCARD_CLOSING_DATE_TABLE, where, args);
	}
	
	public void updateCustomClosingDay(long accountId, int period, int closingDay) {
		// delete previous content
		deleteCustomClosingDay(accountId, period);
		
		// save new value
		setCustomClosingDay(accountId, period, closingDay);
	}

    /**
     * Re-populates running_balance table for all accounts
     */
    public void rebuildRunningBalance() {
        List<Account> accounts = em.getAllAccountsList();
        for (Account account : accounts) {
            rebuildRunningBalanceForAccount(account);
        }
    }

    /**
     * Re-populates running_balance for specific account
     * @param account selected account
     */
    public void rebuildRunningBalanceForAccount(Account account) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            String accountId = String.valueOf(account.getId());
            db.execSQL("delete from running_balance where account_id=?", new Object[]{accountId});
            WhereFilter filter = new WhereFilter("");
            filter.put(WhereFilter.Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, accountId));
            filter.asc("datetime");
            filter.asc("_id");
            Cursor c = getBlotterForAccountWithSplits(filter);
            Object[] values = new Object[4];
            values[0] = accountId;
            try {
                long balance = 0;
                while (c.moveToNext()) {
                    long parentId = c.getLong(BlotterColumns.parent_id.ordinal());
                    if (parentId > 0) {
                        int isTransfer = c.getInt(BlotterColumns.is_transfer.ordinal());
                        if (isTransfer >= 0) {
                            // we only interested in the second part of the transfer-split
                            // which is marked with is_transfer=-1 (see v_blotter_for_account_with_splits)
                            continue;
                        }
                    }
                    balance += c.getLong(DatabaseHelper.BlotterColumns.from_amount.ordinal());
                    values[1] = c.getString(DatabaseHelper.BlotterColumns._id.ordinal());
                    values[2] = c.getString(DatabaseHelper.BlotterColumns.datetime.ordinal());
                    values[3] = balance;
                    db.execSQL("insert into running_balance(account_id,transaction_id,datetime,balance) values (?,?,?,?)", values);
                }
            } finally {
                c.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static final String[] SUM_FROM_AMOUNT = new String[]{"sum(from_amount)"};

    public long fetchBudgetBalance(Map<Long, Category> categories, Map<Long, Project> projects, Budget b) {
        String where = Budget.createWhere(b, categories, projects);
        Cursor c = db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, SUM_FROM_AMOUNT, where, null, null, null, null);
        try {
            if (c.moveToNext()) {
                return c.getLong(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public void recalculateAccountsBalances() {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            Cursor accountsCursor = db.query(ACCOUNT_TABLE, new String[]{AccountColumns.ID}, null, null, null, null, null);
            try {
                while (accountsCursor.moveToNext()) {
                    long accountId = accountsCursor.getLong(0);
                    recalculateAccountsBalances(accountId);
                }
            } finally {
                accountsCursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void recalculateAccountsBalances(long accountId) {
        long amount = fetchAccountBalance(accountId);
        ContentValues values = new ContentValues();
        values.put(AccountColumns.TOTAL_AMOUNT, amount);
        db().update(ACCOUNT_TABLE, values, AccountColumns.ID+"=?", new String[]{String.valueOf(accountId)});
        Log.i("DatabaseImport", "Recalculating amount for "+accountId);
    }

    private long fetchAccountBalance(long accountId) {
        Cursor c = db().query(V_BLOTTER_FOR_ACCOUNT_WITH_SPLITS, new String[]{"SUM(" + BlotterColumns.from_amount + ")"},
                BlotterColumns.from_account_id + "=? and (" + BlotterColumns.parent_id + "=0 or " + BlotterColumns.is_transfer + "=-1)",
                new String[]{String.valueOf(accountId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
            return 0;
        } finally {
            c.close();
        }
    }

    public void saveRate(ExchangeRate r) {
        replaceRate(r, r.date);
    }

    public void replaceRate(ExchangeRate rate, long originalDate) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            deleteRateInTransaction(rate.fromCurrencyId, rate.toCurrencyId, originalDate, db);
            saveBothRatesInTransaction(rate, db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void saveBothRatesInTransaction(ExchangeRate r, SQLiteDatabase db) {
        r.date = DateUtils.atMidnight(r.date);
        saveRateInTransaction(db, r);
        saveRateInTransaction(db, r.flip());
    }

    private void saveRateInTransaction(SQLiteDatabase db, ExchangeRate r) {
        ContentValues values = r.toValues();
        db.insert(EXCHANGE_RATES_TABLE, null, values);
    }

    public ExchangeRate findRate(Currency fromCurrency, Currency toCurrency, long date) {
        long day = DateUtils.atMidnight(date);
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, ExchangeRateColumns.NORMAL_PROJECTION_WHERE,
                new String[]{String.valueOf(fromCurrency.id), String.valueOf(toCurrency.id), String.valueOf(day)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return ExchangeRate.fromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public List<ExchangeRate> findRates(Currency fromCurrency) {
        List<ExchangeRate> rates = new ArrayList<ExchangeRate>();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, ExchangeRateColumns.from_currency_id+"=?",
                new String[]{String.valueOf(fromCurrency.id)}, null, null, ExchangeRateColumns.rate_date+" desc");
        try {
            while (c.moveToNext()) {
                rates.add(ExchangeRate.fromCursor(c));
            }
        } finally {
            c.close();
        }
        return rates;
    }

    public List<ExchangeRate> findRates(Currency fromCurrency, Currency toCurrency) {
        List<ExchangeRate> rates = new ArrayList<ExchangeRate>();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION,
                ExchangeRateColumns.from_currency_id+"=? and "+ExchangeRateColumns.to_currency_id+"=?",
                new String[]{String.valueOf(fromCurrency.id), String.valueOf(toCurrency.id)},
                null, null, ExchangeRateColumns.rate_date+" desc");
        try {
            while (c.moveToNext()) {
                rates.add(ExchangeRate.fromCursor(c));
            }
        } finally {
            c.close();
        }
        return rates;
    }

    public ExchangeRateProvider getLatestRates() {
        LatestExchangeRates m = new LatestExchangeRates();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.LATEST_RATE_PROJECTION, null, null, ExchangeRateColumns.LATEST_RATE_GROUP_BY, null, null);
        fillRatesCollection(m, c);
        return m;
    }

    public ExchangeRateProvider getHistoryRates() {
        HistoryExchangeRates m = new HistoryExchangeRates();
        Cursor c = db().query(EXCHANGE_RATES_TABLE, ExchangeRateColumns.NORMAL_PROJECTION, null, null, null, null, null);
        fillRatesCollection(m, c);
        return m;
    }

    private void fillRatesCollection(ExchangeRatesCollection m, Cursor c) {
        try {
            while (c.moveToNext()) {
                ExchangeRate r = ExchangeRate.fromCursor(c);
                m.addRate(r);
            }
        } finally {
            c.close();
        }
    }

    public void deleteRate(ExchangeRate rate) {
        deleteRate(rate.fromCurrencyId, rate.toCurrencyId, rate.date);
    }

    public void deleteRate(long fromCurrencyId, long toCurrencyId, long date) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            deleteRateInTransaction(fromCurrencyId, toCurrencyId, date, db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void deleteRateInTransaction(long fromCurrencyId, long toCurrencyId, long date, SQLiteDatabase db) {
        long d = DateUtils.atMidnight(date);
        db.delete(EXCHANGE_RATES_TABLE, ExchangeRateColumns.DELETE_CLAUSE,
                new String[]{String.valueOf(fromCurrencyId), String.valueOf(toCurrencyId), String.valueOf(d)});
        db.delete(EXCHANGE_RATES_TABLE, ExchangeRateColumns.DELETE_CLAUSE,
                new String[]{String.valueOf(toCurrencyId), String.valueOf(fromCurrencyId), String.valueOf(d)});
    }

    public Total getAccountsTotalInHomeCurrency() {
        Currency homeCurrency = em.getHomeCurrency();
        return getAccountsTotal(homeCurrency);
    }

    /**
     * Calculates total in every currency for all accounts
     */
    public Total[] getAccountsTotal() {
        List<Account> accounts = em.getAllAccountsList();
        Map<Currency, Total> totalsMap = new HashMap<Currency, Total>();
        for (Account account : accounts) {
            if (account.shouldIncludeIntoTotals()) {
                Currency currency = account.currency;
                Total total = totalsMap.get(currency);
                if (total == null) {
                    total = new Total(currency);
                    totalsMap.put(currency, total);
                }
                total.balance += account.totalAmount;
            }
        }
        Collection<Total> values = totalsMap.values();
        return values.toArray(new Total[values.size()]);
    }

    /**
     * Calculates total in home currency for all accounts
     */
    public Total getAccountsTotal(Currency homeCurrency) {
        ExchangeRateProvider rates = getLatestRates();
        List<Account> accounts = em.getAllAccountsList();
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            if (account.shouldIncludeIntoTotals()) {
                if (account.currency.id == homeCurrency.id) {
                    total = total.add(BigDecimal.valueOf(account.totalAmount));
                } else {
                    ExchangeRate rate = rates.getRate(account.currency, homeCurrency);
                    if (rate == ExchangeRate.NA) {
                        return new Total(homeCurrency, TotalError.lastRateError(account.currency));
                    } else {
                        total = total.add(BigDecimal.valueOf(rate.rate*account.totalAmount));
                    }
                }
            }
        }
        Total result = new Total(homeCurrency);
        result.balance = total.longValue();
        return result;
    }

    public boolean singleCurrencyOnly() {
        long currencyId = getSingleCurrencyId();
        return currencyId > 0;
    }
    
    private long getSingleCurrencyId() {
        Cursor c = db().rawQuery("select distinct "+AccountColumns.CURRENCY_ID+" from "+ACCOUNT_TABLE+
                " where "+AccountColumns.IS_INCLUDE_INTO_TOTALS+"=1 and "+AccountColumns.IS_ACTIVE+"=1", null);
        try {
            if (c.getCount() == 1) {
                c.moveToFirst();
                return c.getLong(0);
            }
            return -1;
        } finally {
            c.close();
        }
    } 

    public void setDefaultHomeCurrency() {
        Currency homeCurrency = em.getHomeCurrency();
        long singleCurrencyId = getSingleCurrencyId();
        if (homeCurrency == Currency.EMPTY && singleCurrencyId > 0) {
            Currency c = em.get(Currency.class, singleCurrencyId);
            c.isDefault = true;
            em.saveOrUpdate(c);
        }
    }

    public void purgeAccountAtDate(Account account, long date) {
        long nearestTransactionId = findNearestOlderTransactionId(account, date);
        if (nearestTransactionId > 0) {
            SQLiteDatabase db = db();
            db.beginTransaction();
            try {
                Transaction newTransaction = createTransactionFromNearest(account, nearestTransactionId);
                deleteOldTransactions(account, date);
                insertWithoutUpdatingBalance(newTransaction);
                db.execSQL(INSERT_RUNNING_BALANCE, new Object[]{account.id, newTransaction.id, newTransaction.dateTime, newTransaction.fromAmount});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private Transaction createTransactionFromNearest(Account account, long nearestTransactionId) {
        Transaction nearestTransaction = em.get(Transaction.class, nearestTransactionId);
        long balance = getAccountBalanceForTransaction(account, nearestTransaction);
        Transaction newTransaction = new Transaction();
        newTransaction.fromAccountId = account.id;
        newTransaction.dateTime = DateUtils.atDayEnd(nearestTransaction.dateTime);
        newTransaction.fromAmount = balance;
        return newTransaction;
    }

    public void deleteOldTransactions(Account account, long date) {
        SQLiteDatabase db = db();
        db.delete("transactions", "from_account_id=? and datetime<=? and is_template=0",
                new String[]{String.valueOf(account.id), String.valueOf(DateUtils.atDayEnd(date))});
        db.delete("running_balance", "account_id=? and datetime<=?",
                new String[]{String.valueOf(account.id), String.valueOf(DateUtils.atDayEnd(date))});
    }

    public long getAccountBalanceForTransaction(Account a, Transaction t) {
        return DatabaseUtils.rawFetchLong(this, "select balance from running_balance where account_id=? and transaction_id=?",
                new String[]{String.valueOf(a.id), String.valueOf(t.id)});
    }

    public long findNearestOlderTransactionId(Account account, long date) {
        return DatabaseUtils.rawFetchLong(this,
                "select _id from v_blotter where from_account_id=? and datetime<=? order by datetime desc limit 1",
                new String[]{String.valueOf(account.id), String.valueOf(DateUtils.atDayEnd(date))});
    }

}

