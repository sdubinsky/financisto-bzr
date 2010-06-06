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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.db.DatabaseHelper.ProjectColumns;
import ru.orangesoftware.financisto.model.Project;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ProjectActivity extends Activity {
	
	public static final String PROJECT_ID_EXTRA = "projectId";
	
	private DatabaseAdapter db;	
	private MyEntityManager em;

	private Project project = new Project();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.project);

		db = new DatabaseAdapter(this);
		db.open();
		
		em = new MyEntityManager(this, db.db());

		Button bOK = (Button)findViewById(R.id.bOK);
		bOK.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				EditText title = (EditText)findViewById(R.id.title);
				project.title = title.getText().toString();
				long id = em.saveOrUpdate(project);
				Intent intent = new Intent();
				intent.putExtra(ProjectColumns.ID, id);
				setResult(RESULT_OK, intent);
				finish();
			}

		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}			
		});
		
		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(PROJECT_ID_EXTRA, -1);
			if (id != -1) {
				project = em.load(Project.class, id);
				editProject();
			}
		}
		
	}

	private void editProject() {
		EditText title = (EditText)findViewById(R.id.title);
		title.setText(project.title);
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
}
