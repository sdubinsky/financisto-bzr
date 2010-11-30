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
package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.EntityListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.model.Project;

import java.util.ArrayList;

public abstract class MyEntityListActivity<T extends MyEntity> extends AbstractListActivity {

	private static final int NEW_ENTITY_REQUEST = 1;
	private static final int EDIT_ENTITY_REQUEST = 2;

    private final Class<T> clazz;

	private ArrayList<T> entities;

	public MyEntityListActivity(Class<T> clazz) {
		super(R.layout.project_list);
        this.clazz = clazz;
	}
	
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);
		entities = loadEntities();
	}

    protected abstract ArrayList<T> loadEntities();

    @Override
	protected void addItem() {
		Intent intent = new Intent(MyEntityListActivity.this, getEditActivityClass());
		startActivityForResult(intent, NEW_ENTITY_REQUEST);
	}

    protected abstract Class<? extends MyEntityActivity> getEditActivityClass();

    @Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new EntityListAdapter<T>(this, entities);
	}

	@Override
	protected Cursor createCursor() {
		return null;
	}
	
	@Override
	public void requeryCursor() {
		entities = loadEntities();
        @SuppressWarnings("unchecked")
        EntityListAdapter<T> a = (EntityListAdapter<T>)adapter;
        a.setEntities(entities);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			requeryCursor();
		}
	}

	@Override
	protected void deleteItem(int position, final long id) {
		em.deleteProject(id);
		requeryCursor();
	}

	@Override
	public void editItem(int position, long id) {
		Intent intent = new Intent(MyEntityListActivity.this, getEditActivityClass());
		intent.putExtra(MyEntityActivity.ENTITY_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_ENTITY_REQUEST);
	}	
	
	@Override
	protected void viewItem(int position, long id) {
		T e = em.load(clazz, id);
		Intent intent = new Intent(this, BlotterActivity.class);
        WhereFilter.Criteria blotterFilter = createBlotterCriteria(e);
        blotterFilter.toIntent(e.title, intent);
		startActivity(intent);
	}

    protected abstract WhereFilter.Criteria createBlotterCriteria(T e);

}
