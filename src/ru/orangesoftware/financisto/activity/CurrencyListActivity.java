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

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.widget.ListAdapter;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.CurrencyListAdapter;
import ru.orangesoftware.financisto.utils.MenuItemInfo;

import java.util.List;

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
        new CurrencySelector(this, em, new CurrencySelector.OnCurrencyCreatedListener() {
            @Override
            public void onCreated(long currencyId) {
                if (currencyId == 0) {
                    Intent intent = new Intent(CurrencyListActivity.this, CurrencyActivity.class);
                    startActivityForResult(intent, NEW_CURRENCY_REQUEST);
                } else {
                    recreateCursor();
                }
            }
        }).show();
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new CurrencyListAdapter(db, this, cursor);
	}

	@Override
	protected Cursor createCursor() {
		return em.getAllCurrencies("name");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			cursor.requery();
		}
	}

	@Override
	protected void deleteItem(View v, int position, long id) {
		if (em.deleteCurrency(id) == 1) {
			cursor.requery();
		} else {
			new AlertDialog.Builder(this)
				.setTitle(R.string.delete)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.currency_delete_alert)
				.setNeutralButton(R.string.ok, null).show();
		}
	}

	@Override
	public void editItem(View v, int position, long id) {
		Intent intent = new Intent(this, CurrencyActivity.class);
		intent.putExtra(CurrencyActivity.CURRENCY_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_CURRENCY_REQUEST);		
	}	
	
	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}		

	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.currency);
	}
}

