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
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SelectTemplateActivity extends TemplatesListActivity {
	
	public static final String TEMPATE_ID = "template_id";

	public SelectTemplateActivity() {
		super(R.layout.templates);
	}

	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		internalOnCreateTemplates();
		Button bEditTemplates = (Button)findViewById(R.id.bEditTemplates);
		bEditTemplates.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
				Intent intent = new Intent(SelectTemplateActivity.this, TemplatesListActivity.class);
				startActivity(intent);
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
	}

	@Override
	protected void viewItem(int position, long id) {
		Intent data = new Intent();
		data.putExtra(TEMPATE_ID, id);
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
