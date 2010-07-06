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
import java.util.Iterator;

import android.database.Cursor;

public class CategoryTree<T extends CategoryEntity<T>> implements Iterable<T> {
	
	private final ArrayList<T> roots;

	public CategoryTree(ArrayList<T> roots) {
		this.roots = roots;
	}
	
	public CategoryTree() {
		this.roots = new ArrayList<T>();
	}
	
	public static <T extends CategoryEntity<T>> CategoryTree<T> createFromCursor(Cursor c, NodeCreator<T> creator) {
		ArrayList<T> roots = new ArrayList<T>();
		T parent = null;
		while (c.moveToNext()) {
			T category = creator.createNode(c);
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
		return new CategoryTree<T>(roots);
	}
	
	public static interface NodeCreator<T> {	
		T createNode(Cursor c);
	}
	
	public HashMap<Long, T> asMap() {
		HashMap<Long, T> map = new HashMap<Long, T>();
		initializeMap(map, this);
		return map;
	}
	
	private void initializeMap(HashMap<Long, T> map, CategoryTree<T> tree) {
		for (T c : tree) {
			map.put(c.id, c);
			if (c.hasChildren()) {
				initializeMap(map, c.children);
			}
		}
	}

	@Override
	public Iterator<T> iterator() {
		return roots.iterator();
	}

	public boolean isEmpty() {
		return roots.isEmpty();
	}
	
	public void add(T child) {
		roots.add(child);
	}

	public int indexOf(T child) {
		return roots.indexOf(child);
	}
	
	public int size() {
		return roots.size();
	}
}
