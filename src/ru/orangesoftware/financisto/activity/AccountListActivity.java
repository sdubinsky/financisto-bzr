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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.AccountListAdapter2;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.BlotterTotalsCalculationTask;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.orb.EntityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class AccountListActivity extends AbstractListActivity {
	
	public AccountListActivity() {
		super(R.layout.account_list);
	}

	private static final int NEW_ACCOUNT_REQUEST = 1;
	private static final int EDIT_ACCOUNT_REQUEST = 2;
	private static final int VIEW_ACCOUNT_REQUEST = 3;
	
	private static final int MENU_UPDATE_BALANCE = MENU_ADD+1;
	private static final int MENU_CLOSE_OPEN_ACCOUNT = MENU_ADD+2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		calculateTotals();
	}
	
	@Override
	public void requeryCursor() {
		super.requeryCursor();
		calculateTotals();
	}
	
	private AccountTotalsCalculationTask totalCalculationTask;

	private void calculateTotals() {
		if (totalCalculationTask != null) {
			totalCalculationTask.stop();
			totalCalculationTask.cancel(true);
		}		
		ViewFlipper totalTextFlipper = (ViewFlipper)findViewById(R.id.flipperTotal);
		TextView totalText = (TextView)findViewById(R.id.total);
		totalCalculationTask = new AccountTotalsCalculationTask(totalTextFlipper, totalText);
		totalCalculationTask.execute((Void[])null);
	}
	
	public class AccountTotalsCalculationTask extends AsyncTask<Void, Total, Total[]> {
		
		private volatile boolean isRunning = true;
		
		private final ViewFlipper totalTextFlipper;
		private final TextView totalText;
		
		public AccountTotalsCalculationTask(ViewFlipper totalTextFlipper, TextView totalText) {
			this.totalTextFlipper = totalTextFlipper;
			this.totalText = totalText;
		}

		@Override
		protected Total[] doInBackground(Void... params) {
			long t0 = System.currentTimeMillis();
			try {
				Cursor c = createCursor();
				try {
					Map<String, Total> map = new HashMap<String, Total>();
					while (c.moveToNext()) {
						Account a = EntityManager.loadFromCursor(c, Account.class);
						if (a.isActive && a.isIncludeIntoTotals) {
							String s = a.currency.symbol;
							Total t = map.get(s);
							if (t == null) {
								t = new Total(a.currency);
								map.put(s, t);
							}
							t.balance += a.totalAmount;
						}
					}
					return map.values().toArray(new Total[map.size()]);
				} finally {
					c.close();
					long t1 = System.currentTimeMillis();
					Log.d("ACCOUNT TOTALS", (t1-t0)+"ms");
				}
			} catch (Exception ex) {
				Log.e("AccountTotals", "Unexpected error", ex);
				return new Total[0];
			}
		}

		@Override
		protected void onPostExecute(Total[] result) {
			if (isRunning) {
				BlotterTotalsCalculationTask.setTotals(AccountListActivity.this, totalTextFlipper, totalText, result);
			}
		}
		
		public void stop() {
			isRunning = false;
		}
		
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		//return new AccountListAdapter(this, R.layout.account_list_item, cursor);
		return new AccountListAdapter2(this, cursor);
	}

	@Override
	protected Cursor createCursor() {
		Cursor c = em.getAllAccounts();
		//DatabaseUtils.dumpCursor(c);
		return c;
	}

	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = super.createContextMenus(id);
		Account a = em.getAccount(id);
		if (a != null && a.isActive) {
			menus.add(new MenuItemInfo(MENU_UPDATE_BALANCE, R.string.update_balance));		
			menus.add(new MenuItemInfo(MENU_CLOSE_OPEN_ACCOUNT, R.string.close_account));
		} else {
			menus.add(new MenuItemInfo(MENU_CLOSE_OPEN_ACCOUNT, R.string.reopen_account));
		}
		return menus;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		switch (item.getItemId()) {
			case MENU_UPDATE_BALANCE: {
				AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				Account a = em.getAccount(mi.id);
				if (a != null) {
					Intent intent = new Intent(this, TransactionActivity.class);
					intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, a.id);
					intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount);
					startActivityForResult(intent, 0);			
					return true;
				}
			} 			
			case MENU_CLOSE_OPEN_ACCOUNT: {
				AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				Account a = em.getAccount(mi.id);
				a.isActive = !a.isActive;
				em.saveAccount(a);
				requeryCursor();
				return true;
			} 			
		}
		return false;
	}
	
	@Override
	protected void addItem() {		
		Intent intent = new Intent(AccountListActivity.this, AccountActivity.class);
		startActivityForResult(intent, NEW_ACCOUNT_REQUEST);
	}

	@Override
	protected void deleteItem(int position, final long id) {
		new AlertDialog.Builder(this)
			.setMessage(R.string.delete_account_confirm)
			.setPositiveButton(R.string.yes, new OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					db.deleteAccount(id);
					requeryCursor();
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
	}

	@Override
	public void editItem(int position, long id) {
		Intent intent = new Intent(AccountListActivity.this, AccountActivity.class);
		intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_ACCOUNT_REQUEST);
	}

	@Override
	protected void viewItem(int position, long id) {
		Account account = em.getAccount(id);
		if (account != null) {
			Intent intent = new Intent(AccountListActivity.this, BlotterActivity.class);
			WhereFilter.Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(id))
				.toIntent(account.title, intent);		
			startActivityForResult(intent, VIEW_ACCOUNT_REQUEST);
		}
	}
	
	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.account);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VIEW_ACCOUNT_REQUEST) {
			requeryCursor();
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected void onDestroy() {
		ViewFlipper totalTextFlipper = (ViewFlipper)findViewById(R.id.flipperTotal);
		if (totalTextFlipper != null) {
			totalTextFlipper.stopFlipping();
		}
		super.onDestroy();
	}
	
}
