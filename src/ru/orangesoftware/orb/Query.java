/*******************************************************************************
 * Copyright 2010 Denis Solonenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ru.orangesoftware.orb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class Query<T> {
	
	private final Class<T> clazz;
	private final EntityDefinition ed;
	private final SQLiteDatabase db;
	
	private final LinkedList<String> orderBy = new LinkedList<String>();
	private String where;
	private String[] whereArgs;
	
	Query(EntityManager em, Class<T> clazz) {		
		this.db = em.db();
		this.clazz = clazz;
		this.ed = EntityManager.getEntityDefinitionOrThrow(clazz);
	}
	
	public Query<T> where(Expression ex) {
		Selection s = ex.toSelection(ed);
		where = s.selection;
		List<String> args = s.selectionArgs; 
		whereArgs = args.toArray(new String[args.size()]);
		return this;
	}
	
	public Query<T> asc(String field) {
		orderBy.add(ed.getColumnForField(field)+" asc");
		return this;
	}
	
	public Query<T> desc(String field) {
		orderBy.add(ed.getColumnForField(field)+" desc");
		return this;
	}

	public Cursor execute() {
		String query = ed.sqlQuery;
		String where = this.where;
		String[] whereArgs = this.whereArgs;
		StringBuilder sb = new StringBuilder(query);
		if (where != null) {
			sb.append(" where ").append(where);			
		}
		if (orderBy.size() > 0) {
			sb.append(" order by ");
			boolean addComma = false;
			for (String order : orderBy) {
				if (addComma) {
					sb.append(", ");
				}
				sb.append(order);
				addComma = true;
			}			
		}
		query = sb.toString();
		Log.d("QUERY "+clazz, query);
		Log.d("WHERE", where != null ? where : "");
		Log.d("ARGS", whereArgs != null ? Arrays.toString(whereArgs) : "");
		return db.rawQuery(query, whereArgs);
	}

	public T uniqueResult() {
		Cursor c = execute();
		try {
			if (c.moveToFirst()) {
				return EntityManager.loadFromCursor(c, clazz);
			}
			return null;
		} finally {
			c.close();
		}
	}

	public List<T> list() {
		Cursor c = execute();
		try {
			List<T> list = new ArrayList<T>();
			while (c.moveToNext()) {
				T e = EntityManager.loadFromCursor(c, clazz);
				list.add(e);
			}
			return list;
		} finally {
			c.close();
		}
	}

}
