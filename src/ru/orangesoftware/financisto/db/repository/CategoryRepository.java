package ru.orangesoftware.financisto.db.repository;

import static ru.orangesoftware.financisto.db.DatabaseHelper.CATEGORY_ATTRIBUTE_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.CATEGORY_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.TRANSACTION_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_CATEGORY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryAttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryViewColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.model.CategoryTree.NodeCreator;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class CategoryRepository {

	private final SQLiteDatabase db;
	
	/*default*/ CategoryRepository(SQLiteDatabase db) {
		this.db = db;
	}
	
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
	
	public long insertChildCategory(long parentId, String title) {
		//DECLARE v_leftkey INT UNSIGNED DEFAULT 0;
		//SELECT l INTO v_leftkey FROM `nset` WHERE `id` = ParentID;
		//UPDATE `nset` SET `r` = `r` + 2 WHERE `r` > v_leftkey;
		//UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_leftkey;
		//INSERT INTO `nset` (`name`, `l`, `r`) VALUES (NodeName, v_leftkey + 1, v_leftkey + 2);
		return insertCategory(CategoryColumns.left.name(), parentId, title);
	}

	public long insertMateCategory(long categoryId, String title) {
		//DECLARE v_rightkey INT UNSIGNED DEFAULT 0;
		//SELECT `r` INTO v_rightkey FROM `nset` WHERE `id` = MateID;
		//UPDATE `	nset` SET `r` = `r` + 2 WHERE `r` > v_rightkey;
		//UPDATE `nset` SET `l` = `l` + 2 WHERE `l` > v_rightkey;
		//INSERT `nset` (`name`, `l`, `r`) VALUES (NodeName, v_rightkey + 1, v_rightkey + 2);
		return insertCategory(CategoryColumns.right.name(), categoryId, title);
	}

	private long insertCategory(String field, long categoryId, String title) {
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
		db.beginTransaction();
		try {
			String[] args = new String[]{String.valueOf(num)};
			db.execSQL(INSERT_CATEGORY_UPDATE_RIGHT, args);
			db.execSQL(INSERT_CATEGORY_UPDATE_LEFT, args);
			ContentValues values = new ContentValues();
			values.put(CategoryColumns.title.name(), title);
			values.put(CategoryColumns.left.name(), num+1);
			values.put(CategoryColumns.right.name(), num+2);
			long id = db.insert(CATEGORY_TABLE, null, values);
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
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
	
	private void updateCategory(long id, String title) {
		ContentValues values = new ContentValues();
		values.put(CategoryColumns.title.name(), title);
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

	public void moveCategory(long id, long newParentId, String title) {
		db.beginTransaction();
		try {
			
			updateCategory(id, title);
			
			long origin_lft, origin_rgt, new_parent_rgt;
			Cursor c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.left.name(), CategoryColumns.right.name()},
					CategoryColumns._id+"=?", new String[]{String.valueOf(id)}, null, null, null);
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
			c = db.query(CATEGORY_TABLE, new String[]{CategoryColumns.right.name()},
					CategoryColumns._id+"=?", new String[]{String.valueOf(newParentId)}, null, null, null);
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
				+CategoryColumns.left+" = "+CategoryColumns.left+" + CASE "
				+" WHEN "+new_parent_rgt+" < "+origin_lft
				+" THEN CASE "
				+" WHEN "+CategoryColumns.left+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+new_parent_rgt+" - "+origin_lft
				+" WHEN "+CategoryColumns.left+" BETWEEN "+new_parent_rgt+" AND "+(origin_lft-1)
				+" THEN "+(origin_rgt-origin_lft+1)
				+" ELSE 0 END "
				+" WHEN "+new_parent_rgt+" > "+origin_rgt
				+" THEN CASE "
				+" WHEN "+CategoryColumns.left+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+(new_parent_rgt-origin_rgt-1)
				+" WHEN "+CategoryColumns.left+" BETWEEN "+(origin_rgt+1)+" AND "+(new_parent_rgt-1)
				+" THEN "+(origin_lft - origin_rgt - 1)
				+" ELSE 0 END "
				+" ELSE 0 END,"
				+CategoryColumns.right+" = "+CategoryColumns.right+" + CASE "
				+" WHEN "+new_parent_rgt+" < "+origin_lft
				+" THEN CASE "
				+" WHEN "+CategoryColumns.right+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+(new_parent_rgt-origin_lft)
				+" WHEN "+CategoryColumns.right+" BETWEEN "+new_parent_rgt+" AND "+(origin_lft - 1)
				+" THEN "+(origin_rgt-origin_lft+1)
				+" ELSE 0 END "
				+" WHEN "+new_parent_rgt+" > "+origin_rgt
				+" THEN CASE "
				+" WHEN "+CategoryColumns.right+" BETWEEN "+origin_lft+" AND "+origin_rgt
				+" THEN "+(new_parent_rgt-origin_rgt-1)
				+" WHEN "+CategoryColumns.right+" BETWEEN "+(origin_rgt+1)+" AND "+(new_parent_rgt-1)
				+" THEN "+(origin_lft-origin_rgt-1)
				+" ELSE 0 END "
				+" ELSE 0 END");
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}


}
