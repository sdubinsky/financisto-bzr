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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.backup.DatabaseImport;
import ru.orangesoftware.financisto.backup.SettingsNotConfiguredException;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.dialog.WebViewDialog;
import ru.orangesoftware.financisto.export.BackupExportTask;
import ru.orangesoftware.financisto.export.CsvExportTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.EntityEnum;
import ru.orangesoftware.financisto.utils.EnumUtils;
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ListAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import api.wireless.gdata.client.AbstructParserFactory;
import api.wireless.gdata.client.GDataParserFactory;
import api.wireless.gdata.client.ServiceDataClient;
import api.wireless.gdata.data.Feed;
import api.wireless.gdata.docs.client.DocsClient;
import api.wireless.gdata.docs.client.DocsGDataClient;
import api.wireless.gdata.docs.data.DocumentEntry;
import api.wireless.gdata.docs.data.FolderEntry;
import api.wireless.gdata.docs.parser.xml.XmlDocsGDataParserFactory;
import api.wireless.gdata.parser.ParseException;
import api.wireless.gdata.util.AuthenticationException;
import api.wireless.gdata.util.ServiceException;

import com.nullwire.trace.ExceptionHandler;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
	
	private static final int ACTIVITY_PIN = 1;
	private static final int ACTIVITY_CSV_EXPORT = 2;
	
	private static final int MENU_PREFERENCES = Menu.FIRST+1;
	private static final int MENU_ABOUT = Menu.FIRST+2;
	private static final int MENU_BACKUP = Menu.FIRST+3;
	private static final int MENU_RESTORE = Menu.FIRST+4;
	private static final int MENU_CSV_EXPORT = Menu.FIRST+5;
	private static final int MENU_SCHEDULED_TRANSACTIONS = Menu.FIRST+6;
	private static final int MENU_BACKUP_GDOCS = Menu.FIRST+7;
	private static final int MENU_RESTORE_GDOCS = Menu.FIRST+8;
	private static final int MENU_ENTITIES = Menu.FIRST+9;
	private static final int MENU_MASS_OP = Menu.FIRST+10;
	
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
				char fieldSeparator = data.getCharExtra(CsvExportActivity.CSV_EXPORT_FIELD_SEPARATOR, ',');
				boolean includeHeader = data.getBooleanExtra(CsvExportActivity.CSV_EXPORT_INCLUDE_HEADER, true);
				currency.symbol = "$";
				currency.decimals = data.getIntExtra(CsvExportActivity.CSV_EXPORT_DECIMALS, 2);
				currency.decimalSeparator = data.getStringExtra(CsvExportActivity.CSV_EXPORT_DECIMAL_SEPARATOR);
				currency.groupSeparator = data.getStringExtra(CsvExportActivity.CSV_EXPORT_GROUP_SEPARATOR);
				doCsvExport(filter, currency, fieldSeparator, includeHeader);
			}
		}
	}
	
	private void doCsvExport(WhereFilter filter, Currency currency, char fieldSeparaotr, boolean includeHeader) {
		ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.csv_export_inprogress), true);
		new CsvExportTask(this, d, filter, currency, fieldSeparaotr, includeHeader).execute((String[])null);
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
		MenuItem menuItem = menu.add(0, MENU_ENTITIES, 0, R.string.entities);
		menuItem.setIcon(R.drawable.menu_entities);
		menuItem = menu.add(0, MENU_SCHEDULED_TRANSACTIONS, 0, R.string.scheduled_transactions);
		menuItem.setIcon(R.drawable.ic_menu_today);
		menuItem = menu.add(0, MENU_MASS_OP, 0, R.string.mass_operations);
		menuItem.setIcon(R.drawable.ic_menu_agenda);
		menuItem = menu.add(0, MENU_BACKUP, 0, R.string.backup_database);
		menuItem.setIcon(R.drawable.ic_menu_upload);
		menuItem = menu.add(0, MENU_PREFERENCES, 0, R.string.preferences);
		menuItem.setIcon(android.R.drawable.ic_menu_preferences);
		menu.addSubMenu(0, MENU_RESTORE, 0, R.string.restore_database);
		menu.addSubMenu(0, MENU_BACKUP_GDOCS, 0, R.string.backup_database_gdocs);
		menu.addSubMenu(0, MENU_RESTORE_GDOCS, 0, R.string.restore_database_gdocs);
		menu.addSubMenu(0, MENU_CSV_EXPORT, 0, R.string.csv_export);
		menu.addSubMenu(0, MENU_ABOUT, 0, R.string.about);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MENU_ENTITIES:
			final MenuEntities[] entities = MenuEntities.values();
			ListAdapter adapter = EnumUtils.createEntityEnumAdapter(this, entities);
			final AlertDialog d = new AlertDialog.Builder(this)
								.setAdapter(adapter, new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										MenuEntities e = entities[which];
										startActivity(new Intent(MainActivity.this, e.getActivityClass()));										
									}
								})
								.create();
			d.setTitle(R.string.entities);
			d.show();
			break;
		case MENU_PREFERENCES:
			startActivity(new Intent(this, PreferencesActivity.class));
			break;
		case MENU_SCHEDULED_TRANSACTIONS:
			startActivity(new Intent(this, ScheduledListActivity.class));
			break;
		case MENU_MASS_OP:
			startActivity(new Intent(this, MassOpActivity.class));
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
		case MENU_BACKUP_GDOCS:
			doBackupOnline();
			break;
		case MENU_RESTORE:
			doImport();
			break;
		case MENU_RESTORE_GDOCS:
			doImportFromGoogleDocs();
			break;
		}
		return false;
	}
	
	/**
	 * Treat asynchronous requests to popup error messages
	 * */
	private Handler handler = new Handler() {
		/**
		 * Schedule the popup of the given error message
		 * @param msg The message to display
		 **/
		@Override
		public void handleMessage(Message msg) {
			showErrorPopup(MainActivity.this, msg.what);
		}
	};
	
	/**
	 * Display the error message
	 * @param context 
	 * @message message The message to display
	 **/
	protected void showErrorPopup(Context context, int message) {
		new AlertDialog.Builder(context)
		.setMessage(message)
		.setTitle(R.string.error)
		.setPositiveButton(R.string.ok, null)
		.setCancelable(true)
		.create().show();
	}
	
	/**
	 * Connects to Google Docs
	 * */
	protected DocsClient createDocsClient(Context context) throws AuthenticationException, SettingsNotConfiguredException
	{
		GDataParserFactory dspf = new XmlDocsGDataParserFactory(new AbstructParserFactory());
		DocsGDataClient dataClient = new DocsGDataClient(
			"cl",
			ServiceDataClient.DEFAULT_AUTH_PROTOCOL, 
			ServiceDataClient.DEFAULT_AUTH_HOST);
		DocsClient googleDocsClient = new DocsClient(dataClient, dspf);

		/*
		 * Start authentication
		 * */
		// check user login on preferences
		String login = MyPreferences.getUserLogin(context);
		if(login==null||login.equals("")) 
			throw new SettingsNotConfiguredException("login");
		// check user password on preferences
		String password = MyPreferences.getUserPassword(context);
		if(password==null||password.equals("")) 
			throw new SettingsNotConfiguredException("password");
		
		googleDocsClient.setUserCredentials(login, password);
		
		return googleDocsClient;
	}
	
	private void doBackup() {
		ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
		new BackupExportTask(this, d).execute((String[])null);
	}
	
	/**
	 * Backup to Google Docs using the Google account parameters registered on preferences.
	 * */
	private void doBackupOnline() {
		ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_gdocs_inprogress), true);
		new OnlineBackupExportTask(d).execute((String[])null);
	}

	private void doCsvExport() {
		Intent intent = new Intent(this, CsvExportActivity.class);
		startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
	}

	private String selectedBackupFile;
	private Properties backupFiles;
	
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
	
	/**
	 * Retrieves the backup file from the Google Docs account registered on preferences
	 * */
	private void doImportFromGoogleDocs() {
		Feed<DocumentEntry> feed = null;
		
		try	{ 
			Context context = MainActivity.this;
			DocsClient docsClient = createDocsClient(context);
			
			// get the list of files in the repository
			String folder = MyPreferences.getBackupFolder(context);
			// check the backup folder registered on preferences
			if(folder==null||folder.equals("")) {
				showErrorPopup(this,R.string.gdocs_folder_not_configured);
				return;
			}
			FolderEntry fd = docsClient.getFolderByTitle(folder);
			if(fd==null) // if the registered folder does not exist
			{
				showErrorPopup(this,R.string.gdocs_folder_not_found);
				return;
			}
			feed = docsClient.getFolderDocsListFeed(fd.getKey());
		}  catch (AuthenticationException e) { //falha de login
			showErrorPopup(this,R.string.gdocs_login_failed);
			return;
		}  catch (SettingsNotConfiguredException e) { //parametros de login nao configurados
			if(e.getMessage().equals("login"))
				handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
			else if(e.getMessage().equals("password"))
				handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
			return;
		} catch (ParseException e) {
			showErrorPopup(this,R.string.gdocs_folder_error);
		} catch (ServiceException e) {
			showErrorPopup(this,R.string.gdocs_service_error);
		} catch (IOException e) {
			showErrorPopup(this,R.string.gdocs_io_error);
		}  catch(Exception e) { //outros erros de conexao
			showErrorPopup(this,R.string.gdocs_connection_failed);
			return;
		}
		
		/*
		 * Convert from ListList<DocumentEntry> to String[] to use in method setSingleChoiceItems()
		 * */
		List<DocumentEntry> arquivos = feed.getEntries();
		final String[] backupFilesNames = new String[arquivos.size()];
		backupFiles = new Properties();
		String name;
		for (int i=0;i<arquivos.size();i++) {
			name = arquivos.get(i).getTitle();
			backupFilesNames[i]=name;
			backupFiles.put(name, arquivos.get(i).getKey());
		}		
		
		new AlertDialog.Builder(this)
			.setTitle(R.string.restore_database)
			.setPositiveButton(R.string.restore, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (selectedBackupFile != null) {
						ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.restore_database_inprogress_gdocs), true);
						new OnlineBackupImportTask(d).execute(selectedBackupFile, backupFiles.get(selectedBackupFile).toString());
					}
				}
			})
			.setSingleChoiceItems(backupFilesNames, -1, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (backupFilesNames != null && which >= 0 && which < backupFilesNames.length) {
						selectedBackupFile = backupFilesNames[which];
					}
				}
			})
			.show();
	}

	/**
	 * Task that calls backup to google docs functions
	 * */
	private class OnlineBackupExportTask extends ImportExportAsyncTask {
		
		public OnlineBackupExportTask(ProgressDialog dialog) {
			super(MainActivity.this, dialog, null);
		}
		
		@Override
		protected Object work(Context context, DatabaseAdapter db, String...params)  throws AuthenticationException, Exception{
			DatabaseExport export = new DatabaseExport(context, db.db());
			try {
				String folder = MyPreferences.getBackupFolder(context);
				// check the backup folder registered on preferences
				if(folder==null||folder.equals("")) {
					throw new SettingsNotConfiguredException("folder-is-null");
				}
				return export.exportOnline(createDocsClient(context), folder);
			}  catch (AuthenticationException e) { // connection error
				handler.sendEmptyMessage(R.string.gdocs_login_failed);
				throw e;
			}  catch (SettingsNotConfiguredException e) { // missing login or password
				if(e.getMessage().equals("login"))
					handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
				else if(e.getMessage().equals("password"))
					handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
				else if(e.getMessage().equals("folder-is-null"))
					handler.sendEmptyMessage(R.string.gdocs_folder_not_configured);
				else if(e.getMessage().equals("folder-not-found"))
					handler.sendEmptyMessage(R.string.gdocs_folder_not_found);
				throw e;
			} catch (ParseException e) {
				handler.sendEmptyMessage(R.string.gdocs_folder_error);
				throw e;
			} catch (NameNotFoundException e) {
				handler.sendEmptyMessage(R.string.package_info_error);
				throw e;
			} catch (ServiceException e) {
				handler.sendEmptyMessage(R.string.gdocs_service_error);
				throw e;
			} catch (IOException e) {
				handler.sendEmptyMessage(R.string.gdocs_io_error);
				throw e;
			}
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
	
	/**
	 * Task that calls backup from google docs functions
	 * */
	private class OnlineBackupImportTask extends ImportExportAsyncTask {
		
		public OnlineBackupImportTask(ProgressDialog dialog) {
			super(MainActivity.this, dialog, new ImportExportAsyncTaskListener(){
				@Override
				public void onCompleted() {
					onTabChanged(getTabHost().getCurrentTabTag());
				}
			});
		}

		@Override
		protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception, AuthenticationException, SettingsNotConfiguredException {
			try {
				new DatabaseImport(MainActivity.this, db.db(), params[0]).
					importOnlineDatabase(createDocsClient(context), params[1]);
			}  catch (SettingsNotConfiguredException e) { // error configuring connection parameters
				if(e.getMessage().equals("login"))
					handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
				else if(e.getMessage().equals("password"))
					handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
				throw e;
			}catch (AuthenticationException e) { // authentication error
				handler.sendEmptyMessage(R.string.gdocs_login_failed);
				throw e;
			} catch (ParseException e) {
				handler.sendEmptyMessage(R.string.gdocs_folder_error);
				throw e;
			} catch (IOException e) {
				handler.sendEmptyMessage(R.string.gdocs_io_error);
				throw e;
			} catch (ServiceException e) {
				handler.sendEmptyMessage(R.string.gdocs_service_error);
				throw e;
			} 
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

	private enum MenuEntities implements EntityEnum {
		
		CURRENCIES(R.string.currencies, R.drawable.menu_entities_currencies, CurrencyListActivity.class),
		CATEGORIES(R.string.categories, R.drawable.menu_entities_categories, CategoryListActivity2.class),
		LOCATIONS(R.string.locations, R.drawable.menu_entities_locations, LocationsListActivity.class),
		PROJECTS(R.string.projects, R.drawable.menu_entities_projects, ProjectListActivity.class);

		private final int titleId;
		private final int iconId;
		private final Class<?> actitivyClass;
		
		private MenuEntities(int titleId, int iconId, Class<?> activityClass) {
			this.titleId = titleId;
			this.iconId = iconId;
			this.actitivyClass = activityClass;
		}
		
		@Override
		public int getTitleId() {
			return titleId;
		}
		
		@Override
		public int getIconId() {
			return iconId;
		}
		
		public Class<?> getActivityClass() {
			return actitivyClass;
		}
		
	}
	
}
