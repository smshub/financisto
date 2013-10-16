/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.flowzr;

import java.io.IOException;
import java.io.InputStream;
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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.FlowzrSyncActivity;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ProgressListener;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class FlowzrSyncTask extends AsyncTask<String, String, Object> {
	protected final Context context;
	protected final ProgressDialog dialog;
    private final FlowzrSyncOptions options;
    private final DefaultHttpClient http_client;
    private final FlowzrSyncActivity flowzrSyncActivity;
    FlowzrSyncEngine flowzrSync;
    
    public FlowzrSyncTask(FlowzrSyncActivity flowzrSyncActivity, Handler handler, ProgressDialog dialog, FlowzrSyncOptions options, DefaultHttpClient pHttp_client) {
        this.options = options;
        this.http_client=pHttp_client;
        this.context=flowzrSyncActivity;
        this.dialog=dialog;        
        this.flowzrSyncActivity=flowzrSyncActivity;
    }

    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
    	
        try {	
        	flowzrSync = new FlowzrSyncEngine(flowzrSyncActivity,context, db, options, http_client);
            flowzrSync.setProgressListener(new ProgressListener() {
                @Override                
                public void onProgress(int percentage) {
                    publishProgress(String.valueOf(percentage));
                }
            });    
            if (checkSubscriptionFromWeb()) {
            	return flowzrSync.doSync();
            } else {
            	return new Exception(context.getString(R.string.flowzr_subscription_required));
            }

            
        } catch (Exception e) {
            return e;
        }
    }

    public boolean checkSubscriptionFromWeb() {
		String url=flowzrSync.FLOWZR_API_URL + "?action=checkSubscription";
	    InputStream isHttpcontent = null;
		try {
          HttpGet httpGet = new HttpGet(url); 
          HttpResponse httpResponse = http_client.execute(httpGet);      
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
        dialog.setProgress(Integer.parseInt(values[0]));        
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
			dialog.setTitle((context.getString(R.string.flowzr_sync_error)));
			dialog.setMessage(((Exception) result).getMessage());
         	dialog.setCancelable(true);
         	dialog.setProgress(100);
         	final String msg=getStackTrace((Exception)result);
         	((Exception)result).printStackTrace();
         	
         	
         	Thread trd = new Thread(new Runnable(){
         		  @Override
         		  public void run(){
         				ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
         				nameValuePairs.add(new BasicNameValuePair("action","error"));
         				nameValuePairs.add(new BasicNameValuePair("stack",msg));					
         		        HttpPost httppost = new HttpPost(flowzrSync.FLOWZR_API_URL + options.useCredential + "/error/");
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
			if (isCancelled()) {
		        flowzrSyncActivity.finish();			
				return;
			}
			flowzrSync.finishDelete();
	    	
	        flowzrSyncActivity.finish();         	
	        Toast.makeText(context.getApplicationContext(), R.string.flowzr_sync_success, Toast.LENGTH_SHORT).show();  
	        		
			dialog.dismiss();			
	        options.lastSyncLocalTimestamp=System.currentTimeMillis();
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putLong(FlowzrSyncActivity.LAST_SYNC_LOCAL_TIMESTAMP, System.currentTimeMillis());
			editor.commit();
		}
	}
}
