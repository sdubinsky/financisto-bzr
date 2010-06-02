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
import java.util.HashMap;

import ru.orangesoftware.financisto.db.DatabaseAdapter;

public class CategoriesTree {

	private final ArrayList<Category> categoriesTree;
	private final HashMap<Long, Category> categoriesMap = new HashMap<Long, Category>();
	
	public CategoriesTree(DatabaseAdapter db) {
		this.categoriesTree = db.getAllCategoriesTree(false);
		initializeMap(categoriesTree);
	}

	private void initializeMap(ArrayList<Category> categories) {
		for (Category c : categories) {
			categoriesMap.put(c.id, c);
			if (c.hasChildren()) {
				initializeMap(c.children);
			}
		}
	}
	
	public ArrayList<Category> getTree() {
		return categoriesTree;
	}
	
	public Category getById(long id) {
		return categoriesMap.get(id);
	}
	
	public Category getParentById(long id) {
		Category c = categoriesMap.get(id);
		return c != null ? c.parent : null;
	}
	
}
