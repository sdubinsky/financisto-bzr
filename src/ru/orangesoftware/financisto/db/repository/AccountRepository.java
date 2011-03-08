package ru.orangesoftware.financisto.db.repository;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.TRANSACTION_ATTRIBUTE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.TRANSACTION_TABLE;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionAttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import android.database.sqlite.SQLiteDatabase;

public class AccountRepository {

	private final SQLiteDatabase db;

	/* default */AccountRepository(SQLiteDatabase db) {
		this.db = db;
	}

	private static final String UPDATE_ORPHAN_TRANSACTIONS_1 = "UPDATE "
			+ TRANSACTION_TABLE + " SET " + TransactionColumns.to_account_id
			+ "=0, " + TransactionColumns.to_amount + "=0 " + "WHERE "
			+ TransactionColumns.to_account_id + "=?";
	private static final String UPDATE_ORPHAN_TRANSACTIONS_2 = "UPDATE "
			+ TRANSACTION_TABLE + " SET " + TransactionColumns.from_account_id
			+ "=" + TransactionColumns.to_account_id + ", "
			+ TransactionColumns.from_amount + "="
			+ TransactionColumns.to_amount + ", "
			+ TransactionColumns.to_account_id + "=0, "
			+ TransactionColumns.to_amount + "=0 " + "WHERE "
			+ TransactionColumns.from_account_id + "=? AND "
			+ TransactionColumns.to_account_id + ">0";

	public int deleteAccount(long id) {
		db.beginTransaction();
		try {
			String[] sid = new String[] { String.valueOf(id) };
			db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_1, sid);
			db.execSQL(UPDATE_ORPHAN_TRANSACTIONS_2, sid);
			db.delete(TRANSACTION_ATTRIBUTE_TABLE,
					TransactionAttributeColumns.TRANSACTION_ID
							+ " in (SELECT _id from " + TRANSACTION_TABLE
							+ " where " + TransactionColumns.from_account_id
							+ "=?)", sid);
			db.delete(TRANSACTION_TABLE, TransactionColumns.from_account_id
					+ "=?", sid);
			int count = db.delete(ACCOUNT_TABLE, "_id=?", sid);
			db.setTransactionSuccessful();
			return count;
		} finally {
			db.endTransaction();
		}

	}

}
