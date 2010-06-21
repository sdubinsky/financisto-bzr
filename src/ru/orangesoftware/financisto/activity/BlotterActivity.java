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
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.adapter.TransactionsListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterTotalsCalculationTask;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.dialog.TransactionInfoDialog;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.service.FinancistoService;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.view.NodeInflater;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class BlotterActivity extends AbstractListActivity {
	
	public static final String SAVE_FILTER = "saveFilter";
	public static final String EXTRA_FILTER_ACCOUNTS = "filterAccounts";
	
	private static final int NEW_TRANSACTION_REQUEST = 1;
	private static final int EDIT_TRANSACTION_REQUEST = 2;
	private static final int NEW_TRANSFER_REQUEST = 3;
	private static final int EDIT_TRANSFER_REQUEST = 4;
	private static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 5;
		
	private static final int FILTER_REQUEST = 6;
	private static final int MENU_DUPLICATE = MENU_ADD+1;
	private static final int MENU_SAVE_AS_TEMPLATE = MENU_ADD+2;
	
	protected ViewFlipper totalTextFlipper;	
	protected TextView totalText;
	protected ImageButton bTransfer;
	protected ImageButton bFilter;	
	protected ImageButton bTemplate;	
	
	private BlotterTotalsCalculationTask calculationTask;

	protected boolean suppressRequery;
	protected boolean saveFilter;
	protected WhereFilter blotterFilter;
	protected boolean filterAccounts = false;
	
    public BlotterActivity(int layoutId) {
		super(layoutId);
	}

    public BlotterActivity() {
		super(R.layout.blotter);
	}
        
	protected void calculateTotals() {	
		if (calculationTask != null) {
			calculationTask.stop();
			calculationTask.cancel(true);
		}
		calculationTask = new BlotterTotalsCalculationTask(
				this, db, blotterFilter, totalTextFlipper, totalText, filterAccounts); 
		calculationTask.execute();
	}
	
	@Override
	public void requeryCursor() {
		super.requeryCursor();
		calculateTotals();
	}
	
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);

		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater = new NodeInflater(layoutInflater);

		bTransfer = (ImageButton)findViewById(R.id.bTransfer);
		bTransfer.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
			}			
		});
		
		bFilter = (ImageButton)findViewById(R.id.bFilter);
		bFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(BlotterActivity.this, BlotterFilterActivity.class);
				blotterFilter.toIntent(intent);
				startActivityForResult(intent, FILTER_REQUEST);
			}
		});
		
		bTemplate = (ImageButton)findViewById(R.id.bTemplate);
		bTemplate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				createFromTemplate();
			}
		});

		totalTextFlipper = (ViewFlipper)findViewById(R.id.flipperTotal);
		totalText = (TextView)findViewById(R.id.total);

		Intent intent = getIntent();
		if (intent != null) {			
			blotterFilter = WhereFilter.fromIntent(intent);
			saveFilter = intent.getBooleanExtra(SAVE_FILTER, false);
			filterAccounts = intent.getBooleanExtra(EXTRA_FILTER_ACCOUNTS, false);
		}
		if (saveFilter && blotterFilter.isEmpty()) {
			blotterFilter = WhereFilter.fromSharedPreferences(getPreferences(0));
		}
		applyFilter();
		calculateTotals();
	}
	
	protected void createFromTemplate() {
		Intent intent = new Intent(this, SelectTemplateActivity.class);
		startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
	}

	@Override
	protected List<MenuItemInfo> createContextMenus(long id) {
		if (blotterFilter.isTemplate() || blotterFilter.isSchedule()) {
			return super.createContextMenus(id);			
		} else {
			List<MenuItemInfo> menus = super.createContextMenus(id);			
			menus.add(new MenuItemInfo(MENU_DUPLICATE, R.string.duplicate));
			menus.add(new MenuItemInfo(MENU_SAVE_AS_TEMPLATE, R.string.save_as_template));
			return menus;
		}
	}
	
	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(blotterFilter.isTemplate() ? R.string.template : R.string.transaction);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (!super.onContextItemSelected(item)) {
			switch (item.getItemId()) {
			case MENU_DUPLICATE: {
				AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				duplicateTransaction(mi.id);
				return true;
			} 			
			case MENU_SAVE_AS_TEMPLATE: {
				AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				db.duplicateTransaction(mi.id, 1);
				Toast.makeText(this, R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
				return true;
			} 			
			}
		}
		return false;
	}

	private void duplicateTransaction(long id) {
		db.duplicateTransaction(id);
		Toast.makeText(this, R.string.duplicate_success, Toast.LENGTH_SHORT).show();
		requeryCursor();
	}

	protected void addItem(int requestId, Class<? extends AbstractTransactionActivity> clazz) {
		Intent intent = new Intent(BlotterActivity.this, clazz);
		long accountId = blotterFilter.getAccountId();
		if (accountId != -1) {
			intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
		}
		intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
		startActivityForResult(intent, requestId);
	}

	@Override
	protected void addItem() {		
		addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
	}
	
	@Override
	protected Cursor createCursor() {
		Cursor c;
		long accountId = blotterFilter.getAccountId();
		if (accountId != -1) {
			c = db.getTransactions(blotterFilter);
		} else {
			c = db.getBlotter(blotterFilter);
		}
		//DatabaseUtils.dumpCursor(c);
		return c;
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		long accountId = blotterFilter.getAccountId();
		if (accountId != -1) {
			return new TransactionsListAdapter(db, this, cursor);
		} else {
			return new BlotterListAdapter(this, cursor);			
		}		
	}
	
	@Override
	protected void deleteItem(int position, final long id) {
		new AlertDialog.Builder(this)
		.setMessage(blotterFilter.isTemplate() ? R.string.delete_template_confirm : R.string.delete_transaction_confirm)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				db.deleteTransaction(id);
				requeryCursor();
				FinancistoService.updateWidget(BlotterActivity.this);
			}
		})
		.setNegativeButton(R.string.no, null)
		.show();
	}

	@Override
	public void editItem(int position, long id) {
		editTransaction(position, id, false);
	}
	
	protected void editTransaction(int position, long id, boolean duplicate) {
		Transaction t = db.getTransaction(id);
		if (t.isTransfer()) {
			Intent intent = new Intent(BlotterActivity.this, TransferActivity.class);
			intent.putExtra(TransferActivity.TRAN_ID_EXTRA, id);
			intent.putExtra(TransferActivity.DUPLICATE_EXTRA, duplicate);
			startActivityForResult(intent, EDIT_TRANSFER_REQUEST);			
		} else {
			Intent intent = new Intent(BlotterActivity.this, TransactionActivity.class);
			intent.putExtra(TransactionActivity.TRAN_ID_EXTRA, id);
			intent.putExtra(TransactionActivity.DUPLICATE_EXTRA, duplicate);
			startActivityForResult(intent, EDIT_TRANSACTION_REQUEST);
		}		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILTER_REQUEST) {
			if (resultCode == RESULT_FIRST_USER) {
				blotterFilter.clear();		
			} else if (resultCode == RESULT_OK) {
				blotterFilter = WhereFilter.fromIntent(data);			
			}	
			if (saveFilter) {
				saveFilter();
			}
			applyFilter();
			recreateCursor();			
		} else if (resultCode == RESULT_OK && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
			long templateId = data.getLongExtra(SelectTemplateActivity.TEMPATE_ID, -1);
			if (templateId > 0) {
				duplicateTransaction(templateId);
			}
		}
		if (resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER) {
			calculateTotals();
		}
	}
	
	private void saveFilter() {
		SharedPreferences preferences = getPreferences(0);
		blotterFilter.toSharedPreferences(preferences);
	}

	private void applyFilter() {
		long accountId = blotterFilter.getAccountId();
		if (accountId != -1) {
			Account a = em.getAccount(accountId);
			bAdd.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
			bTransfer.setVisibility(a != null && a.isActive ? View.VISIBLE : View.GONE);
		}
		String title = blotterFilter.getTitle();
		if (title != null) {
			setTitle(getString(R.string.blotter)+" : "+title);
		}
		bFilter.setImageResource(blotterFilter.isEmpty() ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);
	}

	private NodeInflater inflater;

	@Override
	protected void viewItem(int position, long id) {
		TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(this, position, id, em, inflater);
		transactionInfoView.show(id);
	}
	
}
