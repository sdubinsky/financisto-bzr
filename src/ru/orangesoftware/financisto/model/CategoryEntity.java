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
import javax.persistence.Transient;

public class CategoryEntity<T extends CategoryEntity<T>> extends MyEntity {

	@Transient
	public T parent;
	
	@Column(name = "left")
	public int left;
	
	@Column(name = "right")
	public int right;
	
	@Transient
	public ArrayList<T> children;

	public long getParentId() {
		return parent != null ? parent.id : 0;
	}
	
	@SuppressWarnings("unchecked")
	public void addChild(T category) {
		if (children == null) {
			children = new ArrayList<T>();
		}
		category.parent = (T)this;
		children.add(category);
	}
	
	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}
	
}
