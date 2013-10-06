/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.flowzr;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncEngine;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncOptions;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.FlowzrSyncActivity;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class FlowzrSyncTask extends AsyncTask<String, String, Object> {
	protected final Context context;
    private final FlowzrSyncOptions options;
    private final DefaultHttpClient http_client;
    private final FlowzrSyncActivity flowzrSyncActivity;
    FlowzrSyncEngine flowzrSync;
    public static ProgressDialog mProgress;
	public static final String TAG = "flowzr";

	
    public FlowzrSyncTask(FlowzrSyncActivity flowzrSyncActivity, FlowzrSyncEngine _flowzrSyncEngine, FlowzrSyncOptions options, DefaultHttpClient pHttp_client) {
        this.options = options;
        this.http_client=pHttp_client;
        this.context=flowzrSyncActivity;     
        this.flowzrSyncActivity=flowzrSyncActivity;
        this.flowzrSync=_flowzrSyncEngine;
        mProgress = new ProgressDialog(this.flowzrSyncActivity); 
        mProgress.setIcon(R.drawable.icon);
        mProgress.setTitle(flowzrSyncActivity.getString(R.string.flowzr_sync));
        mProgress.setMessage(flowzrSyncActivity.getString(R.string.flowzr_sync_inprogress));
        mProgress.setCancelable(true);
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);   
        try {
        mProgress.show();
        } catch(Exception e) {
        	Log.e(TAG,"avoid a leaked window");
        }

    }


    
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
    	
    	try {	 
    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_auth_inprogress), 30);
    		//FlowzrBilling flowzrBilling = new FlowzrBilling(flowzrSyncActivity, flowzrSyncActivity.getApplicationContext(), http_client, options.useCredential);  
    		//if (flowzrBilling.checkSubscription()) {  
    		//        	Boolean sync=false;
    		//            if (flowzrBilling!=null) {
    		//            	sync=flowzrBilling.checkSubscription();
    		//            } else {
    		//            	sync=false;
    		//            	return new Exception(context.getString(R.string.flowzr_account_setup));
    		//            }
    		if (this.checkSubscriptionFromWeb()) {
    			return flowzrSync.doSync();
    		} else {
    			flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_subscription_required), 100);
    			flowzrSyncActivity.setRunning();
    			return new Exception(context.getString(R.string.flowzr_subscription_required));
    		}        	
    	} catch (Exception e) {
    		return e;
    	}
    }

    public boolean checkSubscriptionFromWeb() {
    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	String registrationId = prefs.getString(FlowzrSyncOptions.PROPERTY_REG_ID, "");
    	if (registrationId=="") {
    		Log.i(TAG, "Registration not found.");
    	}

    	String url=FlowzrSyncOptions.FLOWZR_API_URL + "?action=checkSubscription&regid=" + registrationId;

    	try {
    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_auth_inprogress), 40);            
    		HttpGet httpGet = new HttpGet(url);
    		HttpResponse httpResponse = http_client.execute(httpGet);      
    		flowzrSyncActivity.notifyUser(flowzrSyncActivity.getString(R.string.flowzr_sync_inprogress), 50);            
    		int code = httpResponse.getStatusLine().getStatusCode();
    		if (code==402) {
    			return false;
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	} 
    	return true;
    }
    
    @Override
	protected Object doInBackground(String... params) {

    	DatabaseAdapter db = new DatabaseAdapter(context);
		db.open();
		try {
			return work(context, db, params);
		} catch(Exception ex){		
			ex.printStackTrace();
			return ex;
		} finally {
			db.close();
		}			
		
	}

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mProgress.setProgress(Integer.parseInt(values[0]));        
    }

    static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }
    
	@Override
	protected void onPostExecute(Object result) {
		
		if (result instanceof Exception)  {			
			
         	final String msg=getStackTrace((Exception)result);
         	((Exception)result).printStackTrace();
         	         	
         	Thread trd = new Thread(new Runnable(){
         		  @Override
         		  public void run(){
         				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
         				nameValuePairs.add(new BasicNameValuePair("action","error"));
         				nameValuePairs.add(new BasicNameValuePair("stack",msg));					
         		        HttpPost httppost = new HttpPost(FlowzrSyncOptions.FLOWZR_API_URL + options.useCredential + "/error/");
         		        try {
         					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
         				} catch (UnsupportedEncodingException e) {
         					e.printStackTrace();
         				}
         		        
         		        try {
         					http_client.execute(httppost);
         				} catch (ClientProtocolException e1) {
         					// TODO Auto-generated catch block
         					e1.printStackTrace();
         				} catch (IOException e1) {
         					// TODO Auto-generated catch block
         					e1.printStackTrace();
         				} catch (Exception e) {
         					e.printStackTrace();
         				}
         		  }
         		});
         	trd.start();
         	
         	return;
		} else {
			flowzrSync.finishDelete();

	        options.lastSyncLocalTimestamp=System.currentTimeMillis();
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putLong(FlowzrSyncOptions.PROPERTY_LAST_SYNC_TIMESTAMP, System.currentTimeMillis());
			editor.commit();
			flowzrSyncActivity.setReady();			
			flowzrSyncActivity.nm.cancel(FlowzrSyncActivity.NOTIFICATION_ID);
 			mProgress.hide();
		}
	}
}

