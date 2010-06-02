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
package ru.orangesoftware.financisto.model;

import java.util.ArrayList;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryViewColumns;
import android.content.ContentValues;
import android.database.Cursor;

@Entity
@Table(name = "category")
public class Category extends MyEntity {
	
	public static final String NO_CATEGORY = "<NO_CATEGORY>";
	
	@Column(name = "left")
	public int left;
	
	@Column(name = "right")
	public int right;
	
	@Column(name = "last_location_id")
	public long lastLocationId;

	@Column(name = "last_project_id")
	public long lastProjectId;

	@Column(name = "sort_order")
	public int sortOrder;

	@Transient
	public Category parent;
	
	@Transient
	public ArrayList<Category> children;

	@Transient
	public int level;
	
	@Transient
	public ArrayList<Attribute> attributes;
	
	public Category(){}
	
	public Category(long id){
		this.id = id;
	}
	
	public long getParentId() {
		return parent != null ? parent.id : 0;
	}
	
	public void addChild(Category category) {
		if (children == null) {
			children = new ArrayList<Category>();
		}
		category.parent = this;
		children.add(category);
	}
	
	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}
	
	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(CategoryColumns.ID, id);
		values.put(CategoryColumns.TITLE, title);
		values.put(CategoryColumns.LEFT, left);
		values.put(CategoryColumns.RIGHT, right);
		return values;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("id=").append(id);
		sb.append(",parentId=").append(getParentId());
		sb.append(",title=").append(title);
		sb.append(",level=").append(level);
		sb.append(",left=").append(left);
		sb.append(",right=").append(right);
		sb.append(",order=").append(sortOrder);
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public String getTitle() {
		return getTitle(title, level);
	}

	public static String getTitle(String title, int level) {
		String span = getTitleSpan(level);
		return span+title;
	}
	
	public static String getTitleSpan(int level) {
		level -= 1;
		if (level <= 0) {
			return "";
		} else if (level == 1) {
			return "-- ";
		} else if (level == 2) {
			return "---- ";
		} else if (level == 3) {
			return "------ ";
		} else {
			StringBuilder sb = new StringBuilder();
			if (level > 0) {
				for (int i=1; i<level; i++) {
					sb.append("--");
				}
			}
			return sb.toString();		
		}
	}

	public static Category formCursor(Cursor c) {
		long id = c.getLong(CategoryViewColumns.Indicies.ID);
		Category cat = new Category();
		cat.id = id;
		cat.title = c.getString(CategoryViewColumns.Indicies.TITLE);
		cat.level = c.getInt(CategoryViewColumns.Indicies.LEVEL);
		cat.left = c.getInt(CategoryViewColumns.Indicies.LEFT);
		cat.right = c.getInt(CategoryViewColumns.Indicies.RIGHT);
		cat.lastLocationId = c.getInt(CategoryViewColumns.Indicies.LAST_LOCATION_ID);
		cat.lastProjectId = c.getInt(CategoryViewColumns.Indicies.LAST_PROJECT_ID);
		cat.sortOrder = c.getInt(CategoryViewColumns.Indicies.SORT_ORDER);
		return cat;
	}
}
