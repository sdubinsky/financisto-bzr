package ru.orangesoftware.financisto.db.repository;

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
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryViewColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionAttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SystemAttribute;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.model.TransactionAttribute;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class TransactionRepository {

	private final Context context;
	private final SQLiteDatabase db;
	
	/*default*/ TransactionRepository(Context context, SQLiteDatabase db) {
		this.context = context;
		this.db = db;
	}
	
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
	public Cursor getAllExpenses(String accountId, String start, String end) {
		// query
		String whereFrom = TransactionColumns.from_account_id +"=? AND "+TransactionColumns.from_amount +"<? AND "+
					   	   TransactionColumns.datetime +">? AND "+TransactionColumns.datetime +"<?";
		
		String whereTo = TransactionColumns.to_account_id +"=? AND "+TransactionColumns.to_amount +"<? AND "+
		   				 TransactionColumns.datetime +">? AND "+TransactionColumns.datetime +"<?";
		try {
			Cursor c1 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					   whereFrom, new String[]{accountId, "0", start, end}, null, null, TransactionColumns.datetime.name());
			
			Cursor c2 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					   whereTo, new String[]{accountId, "0", start, end}, null, null, TransactionColumns.datetime.name());
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
	public Cursor getCredits(String accountId, String start, String end) {
		// query
		String whereFrom = TransactionColumns.from_account_id +"=? AND "+TransactionColumns.from_amount +">? AND "+
					   	   TransactionColumns.datetime +">? AND "+TransactionColumns.datetime +"<? AND "+
					       TransactionColumns.is_ccard_payment +"=?";
		
		String whereTo = TransactionColumns.to_account_id +"=? AND "+TransactionColumns.to_amount +">? AND "+
						 TransactionColumns.datetime +">? AND "+TransactionColumns.datetime +"<? AND "+
						 TransactionColumns.is_ccard_payment +"=?";
		
		try {
			Cursor c1 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					   whereFrom, new String[]{accountId, "0", start, end, "0"}, null, null, TransactionColumns.datetime.name());
			Cursor c2 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					   whereTo, new String[]{accountId, "0", start, end, "0"}, null, null, TransactionColumns.datetime.name());
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
	public Cursor getPayments(String accountId, String start, String end) {
		// query direct payments
		String whereFrom = TransactionColumns.from_account_id +"=? AND "+TransactionColumns.from_amount +">? AND "+
						   TransactionColumns.datetime +">? AND "+TransactionColumns.datetime +"<? AND "+
						   TransactionColumns.is_ccard_payment +"=?";
		
		String whereTo =  TransactionColumns.to_account_id +"=? AND "+TransactionColumns.to_amount +">? AND "+
						  TransactionColumns.datetime +">? AND "+TransactionColumns.datetime +"<? AND "+
						  TransactionColumns.is_ccard_payment +"=?";
		
		try {
			Cursor c1 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
					   	whereFrom, new String[]{accountId, "0", start, end, "1"}, null, null, TransactionColumns.datetime.name());
			Cursor c2 = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
						whereTo, new String[]{accountId, "0", start, end, "1"}, null, null, TransactionColumns.datetime.name());
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
    public Cursor getAllTransactions(String accountId, String start, String end) {
        // query
        String where = "("+TransactionColumns.from_account_id +"=? OR "+TransactionColumns.to_account_id +"=?) AND "+
                       TransactionColumns.datetime +">? AND "+TransactionColumns.datetime +"<?";
        try {
            Cursor c = db.query(TRANSACTION_TABLE, TransactionColumns.NORMAL_PROJECTION, 
                       where, new String[]{accountId, accountId, start, end}, null, null, TransactionColumns.datetime.name());
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
			Transaction transaction = getTransaction(id);
			transaction.lastRecurrence = System.currentTimeMillis();
			updateTransaction(transaction);
			transaction.id = -1;
			transaction.isTemplate = isTemplate;
			transaction.dateTime = System.currentTimeMillis();
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
			transaction.lastRecurrence = 0;
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
	
	public void clearAll(long[] ids) {
		String sql = "UPDATE "+TRANSACTION_TABLE+" SET "+TransactionColumns.status +"='"+TransactionStatus.CL+"'";
		runInTransaction(sql, ids);
	}

	public void reconcileAll(long[] ids) {
		String sql = "UPDATE "+TRANSACTION_TABLE+" SET "+TransactionColumns.status +"='"+TransactionStatus.RC+"'";
		runInTransaction(sql, ids);
	}

	public void deleteAll(long[] ids) {
		String sql = "DELETE FROM "+TRANSACTION_TABLE;
		runInTransaction(sql, ids);
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
	
	public Category getCategory(long id) {
		Cursor c = db.query(V_CATEGORY, new String[]{CategoryViewColumns.left.name(), CategoryViewColumns.right.name()},
				CategoryViewColumns._id+"=?", new String[]{String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToNext()) {				
				Category cat = new Category();
				cat.id = id;
				cat.left = c.getInt(0);
				cat.right = c.getInt(1);
				return cat;
			} else {
				return new Category(-1);
			}
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
