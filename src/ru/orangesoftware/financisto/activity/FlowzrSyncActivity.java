/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.activity;


import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncOptions;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncTask;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;



public class FlowzrSyncActivity extends Activity  {

    public static final int CLASS_ACCOUNT = 1;
    public static final int CLASS_CATEGORY = 2;
    public static final int CLASS_TRANSACTION = 3;
    public static final int CLASS_PAYEE= 4;
    public static final int CLASS_PROJECT = 5;
    public static final String AUTO_SYNC = "AUTO_SYNC";    
    public static final String USE_CREDENTIAL = "USE_CREDENTIAL";   
    public static final String LAST_SYNC_LOCAL_TIMESTAMP = "LAST_SYNC_LOCAL_TIMESTAMP";   
    public static final int FLOWZR_SYNC_REQUEST_CODE = 36;    
    private DatabaseAdapter db;
    private long lastSyncLocalTimestamp=0;
    private Account useCredential;
	DefaultHttpClient http_client = new DefaultHttpClient();
    private ProgressDialog progressDialog ;
	private FlowzrSyncTask t;
	public boolean isCanceled=false;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flowzr_sync);
        restorePreferences();

        db = new DatabaseAdapter(this);
        db.open();
        		
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        final Account[] accounts = accountManager.getAccountsByType("com.google");
		if (accounts.length<1) {
			new AlertDialog.Builder(this)
			.setTitle(getString(R.string.flowzr_sync_error))
			.setMessage(R.string.account_required)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}
			})
			.show();
		}
		RadioGroup radioGroupCredentials = (RadioGroup)findViewById(R.id.radioCredentials);

		//LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(1, 1);
    	//String[] mAccountNames = new String[accounts.length];

		OnClickListener radio_listener = new OnClickListener() {
		    public void onClick(View v) {

		    	RadioButton radioButtonSelected = (RadioButton)findViewById(v.getId());
		    	// ((RadioGroup)findViewById(R.id.radioCredentials)).clearCheck();
		    	//radioButtonSelected.setChecked(true);
		    			    	
		    	for (Account account: accounts) {
		    		if (account.name==radioButtonSelected.getText()) {
		    			useCredential=account;
		    		}
		    	}		    			    	
		    }
		};		

	     for (int i = 0; i < accounts.length; i++) {

	    	 	RadioButton rb = new RadioButton(this);
	            radioGroupCredentials.addView(rb); //, 0, lp); 
	    	 	rb.setOnClickListener(radio_listener);
	        	rb.setText(((Account) accounts[i]).name);
	    		if (useCredential!=null) {	        	
		        	if ( accounts[i].name.equals(useCredential.name)) {
		                rb.toggle(); //.setChecked(true);
		        	} 
	    		}

		}

        	
        Button bOk = (Button) findViewById(R.id.bOK);
        bOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	isCanceled=false;
	        	progressDialog = new ProgressDialog(FlowzrSyncActivity.this);
				progressDialog.setCancelable(false);
	        	progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        	progressDialog.setMessage(getString(R.string.flowzr_sync_inprogress));
	        	progressDialog.setProgress(10);
	        	progressDialog.setTitle(getString(R.string.flowzr_sync));
	        	progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
	        	    @Override
	        	    public void onClick(DialogInterface dialog, int which) {
	                    setResult(RESULT_CANCELED);    
	                    if (t!=null) {
	                    	t.cancel(true);
	                    	
	                    }
	                    isCanceled=true;
	        	        dialog.dismiss();
	        	        finish();
	        	    }
	        	});	        	
            	
     	
	        	//
            	if (useCredential==null) {
            		//progressDialog.dismiss();            		
    				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_choose_account);              		
            	} else if (!isOnline()) {        
            		            		
        			//progressDialog.dismiss();
    				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);                                         				
        		} else {
                    savePreferences();	         			
        			AccountManager.get(getApplicationContext()).getAuthToken(useCredential, "ah", false, new GetAuthTokenCallback(), null);                 			
          			
        		}
            }
        });

        Button bCancel = (Button) findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	isCanceled=true;
                setResult(RESULT_CANCELED);    
                finish();
            }
        });        
        
        TextView textViewAbout = (TextView) findViewById(R.id.aboutServerSync);
        textViewAbout.setOnClickListener(new View.OnClickListener() {        		
        	public void onClick(View v) {
	        		if (isOnline()) {
	            		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.flowzr.com/mobile/"));
	            		startActivity(intent);                                     
	        		} else {         			
	    				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);            			           			
	        		}        			
        		}
		});
    }	
	
	@Override
	protected void onResume() {
		super.onResume();
        restorePreferences();		
	}

	public boolean isOnline() {
	    ConnectivityManager cm =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

	
	private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
		public void run(AccountManagerFuture<Bundle> result) {
			Bundle bundle;
			try {
				bundle = result.getResult();
				Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
				if(intent != null) {
					// User input required
					startActivity(intent);
				} else {
		        	progressDialog.show();  					
                	Toast.makeText(FlowzrSyncActivity.this, R.string.flowzr_sync_inprogress, Toast.LENGTH_SHORT).show();
					onGetAuthToken(bundle);
				}
			} catch (OperationCanceledException e) {
				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);				
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);				
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	protected void onGetAuthToken(Bundle bundle) {
		String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
		
		new GetCookieTask().execute(auth_token);
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
			showErrorPopup(FlowzrSyncActivity.this, msg.what);
		}
	};

	private void showErrorPopup(Context context, int message) {
		new AlertDialog.Builder(context)
		.setMessage(message)
		.setTitle(R.string.error)
		.setPositiveButton(R.string.ok, null)
		.setCancelable(true)
		.create().show();
	}
	
	
	private class GetCookieTask extends AsyncTask<String, Void, Boolean> {
		protected Boolean doInBackground(String... tokens) {
			try {				
				
				http_client.getParams().setParameter("http.protocol.content-charset","UTF-8");
				// Don't follow redirects
				http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
				
				HttpGet http_get = new HttpGet("https://flowzr-hrd.appspot.com/_ah/login?continue=https://flowzr-hrd.appspot.com/&auth=" + tokens[0]);
				HttpResponse response;
				response = http_client.execute(http_get);
				if(response.getStatusLine().getStatusCode() != 302) {
					// Response should be a redirect
					return false;
				}
				for(Cookie cookie : http_client.getCookieStore().getCookies()) {
					if(cookie.getName().equals("ACSID")) {
						return true;
					}
				}
			} catch (ClientProtocolException e) {
				Log.e("financisto",e.getMessage());				
				return false;
			} catch (IOException e) {  				
				Log.e("financisto",e.getMessage());
				return false;
			} finally {
				http_client.getParams().setParameter("http.protocol.content-charset","UTF-8");				
				http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);				
			}
			return false;
		}

		protected void onPostExecute(Boolean result) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());	
			FlowzrSyncOptions options = FlowzrSyncOptions.fromPrefs(preferences);	
			progressDialog.setProgress(5);
			t= new FlowzrSyncTask(FlowzrSyncActivity.this, handler, progressDialog, options,http_client);
			t.execute();   												
		}
	}
    
     
    protected void updateResultIntentFromUi(Intent data) {
        data.putExtra(LAST_SYNC_LOCAL_TIMESTAMP, lastSyncLocalTimestamp);
        data.putExtra(USE_CREDENTIAL, useCredential.name);
    }    

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FLOWZR_SYNC_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {                
                        savePreferences();
            }
        }
    }    
    
    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

	protected void savePreferences() {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putString(USE_CREDENTIAL, useCredential.name);  
  
        CheckBox chk=(CheckBox)findViewById(R.id.chk_sync_from_zero);
        if (chk.isChecked()) {
    		editor.putLong(FlowzrSyncActivity.LAST_SYNC_LOCAL_TIMESTAMP, 0);      	
        }
        editor.commit();          
	}

    protected void restorePreferences() {
		//SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());		
		lastSyncLocalTimestamp=preferences.getLong(LAST_SYNC_LOCAL_TIMESTAMP,0);
        AccountManager accountManager = AccountManager.get(getApplicationContext());
		Account[] accounts = accountManager.getAccountsByType("com.google");
	    for (int i = 0; i < accounts.length; i++) {
	    	 if (preferences.getString(USE_CREDENTIAL,"").equals(((Account) accounts[i]).name)) {
	    		 useCredential=accounts[i];
	    	 }
	     }		 		
	}
}
