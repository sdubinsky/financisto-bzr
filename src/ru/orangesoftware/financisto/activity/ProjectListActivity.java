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

import java.util.ArrayList;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ProjectListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.Project;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;

public class ProjectListActivity extends AbstractListActivity {
	
	private static final int NEW_PROJECT_REQUEST = 1;
	private static final int EDIT_PROJECT_REQUEST = 2;
	
	private ArrayList<Project> projects;

	public ProjectListActivity() {
		super(R.layout.project_list);
	}
	
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);
		projects = em.getAllProjectsList(false);
		//disableMenu(MENU_VIEW);
	}

	@Override
	protected void addItem() {
		Intent intent = new Intent(ProjectListActivity.this, ProjectActivity.class);
		startActivityForResult(intent, NEW_PROJECT_REQUEST);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new ProjectListAdapter(this, projects);
	}

	@Override
	protected Cursor createCursor() {
		return null;
	}
	
	@Override
	public void requeryCursor() {
		projects = em.getAllProjectsList(false);
		((ProjectListAdapter)adapter).setProjects(projects);
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
		Intent intent = new Intent(ProjectListActivity.this, ProjectActivity.class);
		intent.putExtra(ProjectActivity.PROJECT_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_PROJECT_REQUEST);		
	}	
	
	@Override
	protected void viewItem(int position, long id) {
		Project p = em.load(Project.class, id);
		Intent intent = new Intent(this, BlotterActivity.class);
		WhereFilter.Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(id))
			.toIntent(p.title, intent);
		startActivity(intent);
	}	

	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.project);
	}
}
