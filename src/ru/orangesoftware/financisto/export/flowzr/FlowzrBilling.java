package ru.orangesoftware.financisto.export.flowzr;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import ru.orangesoftware.financisto.activity.FlowzrSyncActivity;
import ru.orangesoftware.financisto.export.billing.IabHelper;
import ru.orangesoftware.financisto.export.billing.IabResult;
import ru.orangesoftware.financisto.export.billing.Inventory;
import ru.orangesoftware.financisto.export.billing.Purchase;

public class FlowzrBilling {
	
	boolean mSubscribed = false;
	static final String SKU_FLOWZR = "flowzr_sub";
    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;
    private String TAG ;
    // The helper object
    IabHelper mHelper;
    private Context context;
    private FlowzrSyncActivity flowzrSyncActivity;
	public String FLOWZR_API_URL;
    private DefaultHttpClient http_client;  
    private String payload;
	String base64EncodedPublicKey = "__YOU_PLAY_KEY_HERE__";    	
	  
    
    public FlowzrBilling(FlowzrSyncActivity a, Context context,  DefaultHttpClient pHttp_client,String payload) {
    	this.context=context;
    	this.payload=payload;
    	this.flowzrSyncActivity=a;
    	this.http_client=pHttp_client;
    	this.FLOWZR_API_URL=flowzrSyncActivity.FLOWZR_API_URL;
    	this.TAG=flowzrSyncActivity.TAG;
    }

  	 // Listener that's called when we finish querying the items and subscriptions we own
      IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
          public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
              Log.d(TAG, "Query inventory finished.");
              if (result.isFailure()) {
                  Log.e(TAG,"Failed to query inventory: " + result);
                  return;
              }

              Purchase subscriptionPurchase = inventory.getPurchase(SKU_FLOWZR);

              mSubscribed = (subscriptionPurchase != null && verifyDeveloperPayload(subscriptionPurchase));

              if (!mSubscribed) {

            	if (!mHelper.subscriptionsSupported()) {
                   	Toast.makeText(flowzrSyncActivity, "Subscriptions not supported on your device yet. Sorry!", Toast.LENGTH_SHORT).show();                		
                    return;
                  }
             
                  mHelper.launchPurchaseFlow(flowzrSyncActivity,
                          SKU_FLOWZR, IabHelper.ITEM_TYPE_SUBS, 
                          RC_REQUEST, mPurchaseFinishedListener, flowzrSyncActivity.flowzrSyncTask.flowzrSync.options.useCredential.toString());                	            	            	
              } else {
            	  // You'he got a signed response.
            	  informWebOfSubscription(subscriptionPurchase);
            	  // Checked for signed validity on the server side
            	  // Server is informed anyway so can recover on information error
            	  // Server may not send all data, but User can still export it or use Financisto
              }
              Log.d(TAG, "Initial inventory query finished; enabling main UI.");
          }
      };	
      
      // Callback for when a purchase is finished
      IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
          public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
              Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
              if (result.isFailure()) {
            	  Log.d(TAG, "Purchase failure:" + result);
                  return;
              }
              
              if (!verifyDeveloperPayload(purchase)) {
                  return;
              }  			
              Log.d(TAG, "Purchase successful.");              
              if (purchase.getSku().equals(SKU_FLOWZR)) {
                  // bought the subscription
                  Log.d(TAG, "Subscription purchased.");
                  mSubscribed = true;
                  informWebOfSubscription(purchase);
              }
          }
      };
      
      /** Verifies the developer payload of a purchase. */
      boolean verifyDeveloperPayload(Purchase p) {
          String returnedPayload = p.getDeveloperPayload();
          return returnedPayload==payload;
      }
      
      public boolean launchPlayFlow() {    	
    	  mHelper = new IabHelper(context, base64EncodedPublicKey);
           
           // enable debug logging (for a production application, you should set this to false).
           mHelper.enableDebugLogging(true);
           
           // Start setup. This is asynchronous and the specified listener
           // will be called once setup completes.
           Log.d(TAG, "Starting setup.");
           mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
               public void onIabSetupFinished(IabResult result) {
                   Log.d(TAG, "Setup finished.");

                   if (!result.isSuccess()) {
                      // Oh noes, there was a problem.
                   	//Toast.makeText(flowzrSyncActivity, "Problem setting up in-app billing: " + result, Toast.LENGTH_SHORT).show();                    
                      return;
                   }

                   // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                   Log.d(TAG, "Setup successful. Querying inventory.");
                   mHelper.queryInventoryAsync(mGotInventoryListener);
               }
           });
           return true;
       }

      public boolean checkSubscriptionFromWeb() {
  		String url=FLOWZR_API_URL + "?action=checkSubscription";
  	    InputStream isHttpcontent = null;
  		try {
            HttpGet httpGet = new HttpGet(url); 
            HttpResponse httpResponse = http_client.execute(httpGet);      
            int code = httpResponse.getStatusLine().getStatusCode();
            if (code==202) {
            	return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
      	return false;
      }

      public boolean informWebOfSubscription(Purchase p) {
  		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
  		nameValuePairs.add(new BasicNameValuePair("action","gotSubscription"));
  		nameValuePairs.add(new BasicNameValuePair("payload",p.getDeveloperPayload()));
  		nameValuePairs.add(new BasicNameValuePair("purchase",p.getOriginalJson()));
 		
        HttpPost httppost = new HttpPost(FLOWZR_API_URL);
        try {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;				
		}
        HttpResponse response;
        String strResponse;
		try {
			response = http_client.execute(httppost);
	        HttpEntity entity = response.getEntity();
            int code = response.getStatusLine().getStatusCode();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			strResponse = reader.readLine(); 				 			
	        entity.consumeContent();			
	        if (code!=200) {
	        	return false;
	        }

		} catch (ClientProtocolException e) {
			e.printStackTrace();				
			return false;				
		} catch (IOException e) {				
			e.printStackTrace();
			return false;							
		}
		return true;
      }      
      
      public boolean checkSubscription()  {
    	if (!mSubscribed) {
    		if (!checkSubscriptionFromWeb()) {
    			launchPlayFlow();	 
    			mSubscribed=false;
      			return false;      		
      		} else {
      			mSubscribed=true;
      			return true;
      		}
    	} else {
    		mSubscribed=true;
    		return true;
    	}
      }
      
}
