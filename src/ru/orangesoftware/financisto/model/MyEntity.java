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

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;

import ru.orangesoftware.financisto.utils.Utils;

public class MyEntity implements MultiChoiceItem {

	@Id
	@Column(name = "_id")
	public long id = -1;

	@Column(name = "title")
	public String title;
	
	@Transient
	public boolean checked;

	@Override
	public long getId() {
		return id;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public boolean isChecked() {
		return checked;
	}

	@Override
	public void setChecked(boolean checked) {
		this.checked = checked;
	}
	
	@Override
	public String toString() {
		return title;
	}

	public static long[] splitIds(String s) {
		if (Utils.isEmpty(s)) {
			return null;
		}
		String[] a = s.split(",");
		int count = a.length;
		long[] ids = new long[count];
		for (int i=0; i<count; i++) {
			ids[i] = Long.parseLong(a[i]);
		}
		return ids;
	}
	
	public static <T extends MyEntity> HashMap<Long, T> asMap(ArrayList<T> list) {
		HashMap<Long, T> map = new HashMap<Long, T>();
		for (T e : list) {
			map.put(e.id, e);
		}
		return map;
	}

	public static int indexOf(ArrayList<? extends MyEntity> entities, long id) {
		if (entities != null) {
			int count = entities.size();
			for (int i=0; i<count; i++) {
				if (entities.get(i).id == id) {
					return i;
				}
			}
		}
		return -1;
	}

	public static <T extends MyEntity> T find(ArrayList<T> entities, long id) {
		for (T e : entities) {
			if (e.id == id) {
				return e;
			}
		}
		return null;
	}
	
}
