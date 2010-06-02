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
package ru.orangesoftware.financisto.adapter;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.widget.ResourceCursorAdapter;

public abstract class MyResourceCursorAdapter extends ResourceCursorAdapter {

	protected final DatabaseAdapter db;
	protected final MyEntityManager em;
	protected final Utils u;

	public MyResourceCursorAdapter(DatabaseAdapter db, Context context, int layout, Cursor c) {
		super(context, layout, c);			
		this.db = db;
		this.em = new MyEntityManager(context, db.db());
		this.u = new Utils(context);		
	}

}
