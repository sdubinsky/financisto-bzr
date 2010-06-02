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

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.R.id;
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.adapter.CategoryListAdapter;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;

public class CategoryListActivity extends AbstractListActivity {
	
	private static final int NEW_CATEGORY_REQUEST = 1;
	private static final int EDIT_CATEGORY_REQUEST = 2;

	public CategoryListActivity() {
		super(R.layout.category_list);
	}
	
	@Override
	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = super.createContextMenus(id);
		for (MenuItemInfo m : menus) {
			if (m.menuId == MENU_VIEW) {
				m.enabled = false;
				break;
			}
		}
		return menus;
	}

	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);
		ImageButton bAttributes = (ImageButton)findViewById(R.id.bAttributes);
		bAttributes.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(CategoryListActivity.this, AttributeListActivity.class);
				startActivityForResult(intent, 0);
			}
		});
	}

	@Override
	protected void addItem() {
		Intent intent = new Intent(CategoryListActivity.this, CategoryActivity.class);
		startActivityForResult(intent, NEW_CATEGORY_REQUEST);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		CategoryListAdapter adapter = new CategoryListAdapter(db, this, R.layout.category_list_item, cursor);
		adapter.fetchAttributes();
		return adapter;
	}

	@Override
	public void requeryCursor() {
		((CategoryListAdapter)adapter).fetchAttributes();
		super.requeryCursor();
	}

	@Override
	protected Cursor createCursor() {
		Cursor c = db.getAllCategories(false);
		return c;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		requeryCursor();
	}

	@Override
	protected void deleteItem(int position, final long id) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.delete)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(R.string.delete_category_dialog)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					db.deleteCategory(id);
					cursor.requery();
				}				
			})
			.setNegativeButton(R.string.no, null)
			.show();		
	}

	@Override
	public void editItem(int position, long id) {
		Intent intent = new Intent(CategoryListActivity.this, CategoryActivity.class);
		intent.putExtra(CategoryActivity.CATEGORY_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_CATEGORY_REQUEST);		
	}

	@Override
	protected void viewItem(int position, long id) {
		editItem(position, id);
		
	}	
	
	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.category);
	}
	
}
