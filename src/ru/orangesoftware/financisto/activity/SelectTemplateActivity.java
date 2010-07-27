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
import ru.orangesoftware.financisto.adapter.TemplateListAdapter;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;

public class SelectTemplateActivity extends TemplatesListActivity {
	
	public static final String TEMPATE_ID = "template_id";
	public static final String MULTIPLIER = "multiplier";

	public SelectTemplateActivity() {
		super(R.layout.templates);
	}
	
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		internalOnCreateTemplates();
		Button b = (Button)findViewById(R.id.bEditTemplates);
		b.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
				Intent intent = new Intent(SelectTemplateActivity.this, TemplatesListActivity.class);
				startActivity(intent);
			}
		});
		b = (Button)findViewById(R.id.bCancel);
		b.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		ImageButton ib = (ImageButton)findViewById(R.id.bPlus);
		ib.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				((TemplateListAdapter)getListAdapter()).incrementMultiplier();
			}
		});
		ib = (ImageButton)findViewById(R.id.bMinus);
		ib.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				((TemplateListAdapter)getListAdapter()).decrementMultiplier();
			}
		});
	}

	@Override
	public void registerForContextMenu(View view) {
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new TemplateListAdapter(this, cursor);
	}

	@Override
	protected void viewItem(int position, long id) {
		Intent data = new Intent();
		data.putExtra(TEMPATE_ID, id);
		data.putExtra(MULTIPLIER, ((TemplateListAdapter)getListAdapter()).getMultiplier());
		setResult(RESULT_OK, data);
		finish();
		
	}

	@Override
	public void editItem(int position, long id) {
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// do nothing
	}

}
