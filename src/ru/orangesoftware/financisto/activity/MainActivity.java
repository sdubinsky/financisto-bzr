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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.backup.DatabaseImport;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.dialog.WebViewDialog;
import ru.orangesoftware.financisto.export.CSVExport;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.orb.EntityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.nullwire.trace.ExceptionHandler;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
	
	private static final int ACTIVITY_PIN = 1;
	private static final int ACTIVITY_CSV_EXPORT = 2;
	
	private static final int MENU_PREFERENCES = Menu.FIRST+1;
	private static final int MENU_ABOUT = Menu.FIRST+2;
	private static final int MENU_CURRENCIES = Menu.FIRST+3;
	private static final int MENU_CATEGORIES = Menu.FIRST+4;
	private static final int MENU_LOCATIONS = Menu.FIRST+5;
	private static final int MENU_PROJECTS = Menu.FIRST+6;
	private static final int MENU_BACKUP = Menu.FIRST+7;
	private static final int MENU_RESTORE = Menu.FIRST+8;
	private static final int MENU_CSV_EXPORT = Menu.FIRST+9;
	private static final int MENU_SCHEDULED_TRANSACTIONS = Menu.FIRST+10;

	private final HashMap<String, Boolean> started = new HashMap<String, Boolean>();

	private String appVersion;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Boolean isPinProtected = (Boolean)getLastNonConfigurationInstance();
		if (isPinProtected == null) {
			isPinProtected = true;
		}
		
		if (isPinProtected && MyPreferences.isPinProtected(this)) {
			Intent intent = new Intent(this, PinActivity.class);
			startActivityForResult(intent, ACTIVITY_PIN);
		} else {
			initialLoad();			
		}
		
		if (MyPreferences.isSendErrorReport(this)) {
			ExceptionHandler.register(this, "http://orangesoftware.ru/bugs/server.php");		
		}
		
		final TabHost tabHost = getTabHost();
		
		setupAccountsTab(tabHost);
		setupBlotterTab(tabHost);
		setupBudgetsTab(tabHost);
		setupReportsTab(tabHost);
		
		started.put("accounts", Boolean.TRUE);
		tabHost.setOnTabChangedListener(this);		
    }
			
	@Override  
    public Object onRetainNonConfigurationInstance() {   
        return MyPreferences.isPinProtected(this);   
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_PIN) {
			if (resultCode == RESULT_OK && data != null && data.hasExtra(PinActivity.SUCCESS)) {
				initialLoad();			
			} else {		
				finish();
				System.exit(-1);
			}
		} else if (requestCode == ACTIVITY_CSV_EXPORT) {
			if (resultCode == RESULT_OK) {
				WhereFilter filter = WhereFilter.fromIntent(data);
				Currency currency = new Currency();
				currency.symbol = "$";
				currency.decimals = data.getIntExtra(CsvExportActivity.CSV_EXPORT_DECIMALS, 2);
				currency.decimalSeparator = data.getStringExtra(CsvExportActivity.CSV_EXPORT_DECIMAL_SEPARATOR);
				currency.groupSeparator = data.getStringExtra(CsvExportActivity.CSV_EXPORT_GROUP_SEPARATOR);
				doCsvExport(filter, currency);
			}
		}
	}
	
	private void doCsvExport(WhereFilter filter, Currency currency) {
		ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.csv_export_inprogress), true);
		new CsvExportTask(d, filter, currency).execute((String[])null);
	}
	
	private void initialLoad() {
		long t2, t1, t0 = System.currentTimeMillis();
		DatabaseAdapter db = new DatabaseAdapter(this);
		db.open();
		try {		
			SQLiteDatabase x = db.db();			
			x.beginTransaction();
			t1 = System.currentTimeMillis();
			try {
				updateZero(x, DatabaseHelper.CATEGORY_TABLE, "title", getString(R.string.no_category));
				updateZero(x, DatabaseHelper.PROJECT_TABLE, "title", getString(R.string.no_project));
				updateZero(x, DatabaseHelper.LOCATIONS_TABLE, "name", getString(R.string.current_location));
				x.setTransactionSuccessful();
			} finally {
				x.endTransaction();
			}
			t2 = System.currentTimeMillis();
			EntityManager em = new MyEntityManager(this, x);
			CurrencyCache.initialize(em);
		} finally {
			db.close();
		}
		long t3 = System.currentTimeMillis();
		Log.d("LOADTIME", (t3 - t0)+"ms = "+(t2-t1)+"ms+"+(t3-t2)+"ms");		
		appVersion = WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this);			
	}

	private void updateZero(SQLiteDatabase db, String table, String field, String value) {
		ContentValues values = new ContentValues();
		values.put(field, value);
		db.update(table, values, "_id=0", null);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MyPreferences.setPinRequired(true);
	}
	
	@Override
	public void onTabChanged(String tabId) {
		if (started.containsKey(tabId)) {
			Context c = getTabHost().getCurrentView().getContext();
			if (c instanceof RequeryCursorActivity) {
				long t0 = System.currentTimeMillis();
				((RequeryCursorActivity)c).requeryCursor();
				long t1 = System.currentTimeMillis();
				Log.d("", "onTabChanged "+tabId+" in "+(t1-t0)+"ms");
			}
		} else {
			started.put(tabId, Boolean.TRUE);
		}
	}

	private void setupAccountsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("accounts")
                .setIndicator(getString(R.string.accounts), getResources().getDrawable(R.drawable.ic_tab_accounts))
                .setContent(new Intent(this, AccountListActivity.class)));
	}

    private void setupBlotterTab(TabHost tabHost) {
    	Intent intent = new Intent(this, BlotterActivity.class);
    	intent.putExtra(BlotterActivity.SAVE_FILTER, true);
    	intent.putExtra(BlotterActivity.EXTRA_FILTER_ACCOUNTS, true);    	
        tabHost.addTab(tabHost.newTabSpec("blotter")
                .setIndicator(getString(R.string.blotter), getResources().getDrawable(R.drawable.btn_menu))
                .setContent(intent));
	}

    private void setupBudgetsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("budgets")
                .setIndicator(getString(R.string.budgets), getResources().getDrawable(R.drawable.ic_tab_budget))
                .setContent(new Intent(this, BudgetListActivity.class)));
	}

    private void setupReportsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("reports")
                .setIndicator(getString(R.string.reports), getResources().getDrawable(R.drawable.ic_tab_graph))
                .setContent(new Intent(this, ReportsListActivity.class)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menuItem = menu.add(0, MENU_CURRENCIES, 0, R.string.currencies);
		menuItem.setIcon(R.drawable.icon_currency);
		menuItem = menu.add(0, MENU_CATEGORIES, 0, R.string.categories);
		menuItem.setIcon(R.drawable.ic_menu_categories);
		menuItem = menu.add(0, MENU_LOCATIONS, 0, R.string.locations);
		menuItem.setIcon(android.R.drawable.ic_menu_mylocation);		
		menuItem = menu.add(0, MENU_PROJECTS, 0, R.string.projects);
		menuItem.setIcon(R.drawable.ic_menu_project);		
		menuItem = menu.add(0, MENU_SCHEDULED_TRANSACTIONS, 0, R.string.scheduled_transactions);
		menuItem.setIcon(android.R.drawable.ic_menu_today);
		menuItem = menu.add(0, MENU_PREFERENCES, 0, R.string.preferences);
		menuItem.setIcon(android.R.drawable.ic_menu_preferences);
		menu.addSubMenu(0, MENU_CSV_EXPORT, 0, R.string.csv_export);
		menu.addSubMenu(0, MENU_BACKUP, 0, R.string.backup_database);
		menu.addSubMenu(0, MENU_RESTORE, 0, R.string.restore_database);
		menuItem = menu.add(0, MENU_ABOUT, 0, R.string.about);
		menuItem.setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MENU_CATEGORIES:
			startActivity(new Intent(this, CategoryListActivity.class));
			break;
		case MENU_CURRENCIES:
			startActivity(new Intent(this, CurrencyListActivity.class));
			break;
		case MENU_PREFERENCES:
			startActivity(new Intent(this, PreferencesActivity.class));
			break;
		case MENU_LOCATIONS:
			startActivity(new Intent(this, LocationsListActivity.class));
			break;
		case MENU_PROJECTS:
			startActivity(new Intent(this, ProjectListActivity.class));
			break;
		case MENU_SCHEDULED_TRANSACTIONS:
			startActivity(new Intent(this, ScheduledListActivity.class));
			break;
		case MENU_ABOUT:
			showDialog(1);
			break;
		case MENU_CSV_EXPORT:
			doCsvExport();
			break;
		case MENU_BACKUP:
			doBackup();
			break;
		case MENU_RESTORE:
			doImport();
			break;
		}
		return false;
	}
	
	private void doBackup() {
		ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
		new BackupExportTask(d).execute((String[])null);
	}
	
	private void doCsvExport() {
		Intent intent = new Intent(this, CsvExportActivity.class);
		startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
	}

	private String selectedBackupFile;
	
	private void doImport() {
		final String[] backupFiles = Backup.listBackups();
		new AlertDialog.Builder(this)
			.setTitle(R.string.restore_database)
			.setPositiveButton(R.string.restore, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (selectedBackupFile != null) {
						ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.restore_database_inprogress), true);
						new BackupImportTask(d).execute(selectedBackupFile);
					}
				}
			})
			.setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (backupFiles != null && which >= 0 && which < backupFiles.length) {
						selectedBackupFile = backupFiles[which];
					}
				}
			})
			.show();
	}
	

	private class CsvExportTask extends ImportExportAsyncTask {
		
		private final WhereFilter filter; 
		private final Currency currency;
		
		public CsvExportTask(ProgressDialog dialog, WhereFilter filter, Currency currency) {
			super(MainActivity.this, dialog, null);
			this.filter = filter;
			this.currency = currency;
		}
		
		@Override
		protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
			CSVExport export = new CSVExport(db, filter, currency);
			return export.export();
		}

		@Override
		protected String getSuccessMessage(Object result) {
			return String.valueOf(result);
		}

	}

	private class BackupExportTask extends ImportExportAsyncTask {
		
		public BackupExportTask(ProgressDialog dialog) {
			super(MainActivity.this, dialog, null);
		}
		
		@Override
		protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
			DatabaseExport export = new DatabaseExport(context, db.db());
			return export.export();
		}

		@Override
		protected String getSuccessMessage(Object result) {
			return String.valueOf(result);
		}

	}

	private class BackupImportTask extends ImportExportAsyncTask {
		
		public BackupImportTask(ProgressDialog dialog) {
			super(MainActivity.this, dialog, new ImportExportAsyncTaskListener(){
				@Override
				public void onCompleted() {
					onTabChanged(getTabHost().getCurrentTabTag());
				}
			});
		}

		@Override
		protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
			new DatabaseImport(MainActivity.this, db.db(), params[0]).importDatabase();
			return true;
		}

		@Override
		protected String getSuccessMessage(Object result) {
			return MainActivity.this.getString(R.string.restore_database_success);
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == 1) {
			LayoutInflater inflater = getLayoutInflater(); 
			LinearLayout layout = new LinearLayout(this);
			inflater.inflate(R.layout.about, layout);
			((TextView)layout.findViewById(R.id.appVersion)).setText(appVersion);
			((Button)layout.findViewById(R.id.bWhatsNew)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					WebViewDialog.showWhatsNew(MainActivity.this);
				}
			});
			((ImageButton)layout.findViewById(R.id.bTwitter)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/financisto"));
					startActivity(intent);					
				}
			});
			((Button)layout.findViewById(R.id.bCredits)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					WebViewDialog.showCredits(MainActivity.this);
				}
			});
			Dialog d = new AlertDialog.Builder(this)
				.setIcon(R.drawable.icon)
				.setTitle(R.string.app_name)
				.setView(layout)
				.create();
			d.setCanceledOnTouchOutside(true);
			return d;
		}
		return super.onCreateDialog(id);
	}

}
