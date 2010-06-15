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

import android.database.Cursor;

public abstract class CategoryTree<T extends CategoryEntity<T>> {
	
	public ArrayList<T> create(Cursor c) {
		ArrayList<T> roots = new ArrayList<T>();
		T parent = null;
		while (c.moveToNext()) {
			T category = createNode(c);
			while (parent != null) {
				if (category.left > parent.left && category.right < parent.right) {
					parent.addChild(category);
					break;
				} else {
					parent = parent.parent;
				}										
			}
			if (parent == null) {
				roots.add(category);
			}
			if (category.id > 0 && (category.right - category.left > 1)) {
				parent = category;
			}
		}	
		return roots;
	}
	
	protected abstract T createNode(Cursor c);
	
}
