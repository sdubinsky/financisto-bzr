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
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.adapter.CurrencyListAdapter;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.widget.ListAdapter;

public class CurrencyListActivity extends AbstractListActivity {
	
	private static final int NEW_CURRENCY_REQUEST = 1;
	private static final int EDIT_CURRENCY_REQUEST = 2;

	public CurrencyListActivity() {
		super(R.layout.currency_list);
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
	protected void addItem() {
		Intent intent = new Intent(this, CurrencyActivity.class);
		startActivityForResult(intent, NEW_CURRENCY_REQUEST);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new CurrencyListAdapter(db, this, cursor);
	}

	@Override
	protected Cursor createCursor() {
		Cursor c = em.getAllCurrencies("title");
		//DatabaseUtils.dumpCursor(c);
		return c;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			cursor.requery();
		}
	}

	@Override
	protected void deleteItem(int position, long id) {
		if (em.deleteCurrency(id) == 1) {
			cursor.requery();
		} else {
			new AlertDialog.Builder(this)
				.setTitle(R.string.delete)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.currency_delete_alert)
				.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
					}				
				}).show();		
		}
	}

	@Override
	public void editItem(int position, long id) {
		Intent intent = new Intent(this, CurrencyActivity.class);
		intent.putExtra(CurrencyActivity.CURRENCY_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_CURRENCY_REQUEST);		
	}	
	
	@Override
	protected void viewItem(int position, long id) {
		editItem(position, id);
	}		

	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.currency);
	}
}

