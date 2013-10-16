/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.flowzr;

import org.apache.http.impl.client.DefaultHttpClient;

import ru.orangesoftware.financisto.activity.FlowzrSyncActivity;
import android.content.Intent;
import android.content.SharedPreferences;

public class FlowzrSyncOptions {

	public long lastSyncLocalTimestamp=-1; //zero is default server ...
	public long startTimestamp=-1; //usefull only for not pushing what have just been pooled
	public String useCredential;
	public 	DefaultHttpClient http_client;
	
	
    public FlowzrSyncOptions(String strUseCredential, long lastSyncLocalTimestamp, DefaultHttpClient pHttp_client) {
        this.lastSyncLocalTimestamp = lastSyncLocalTimestamp;
        this.useCredential=strUseCredential;
        this.http_client=pHttp_client;
    }

    public static FlowzrSyncOptions fromIntent(Intent data) {
    	String strUseCredential=null;
    	long lastSyncLocalTimestamp = 0;
    	strUseCredential=data.getStringExtra(FlowzrSyncActivity.USE_CREDENTIAL);  	
    	lastSyncLocalTimestamp = data.getLongExtra(FlowzrSyncActivity.LAST_SYNC_LOCAL_TIMESTAMP,0);
        return new FlowzrSyncOptions(strUseCredential,lastSyncLocalTimestamp,null);
    }

    public static FlowzrSyncOptions fromPrefs(SharedPreferences preferences) {
    	long lastSyncLocalTimestamp=preferences.getLong(FlowzrSyncActivity.LAST_SYNC_LOCAL_TIMESTAMP,0);    	
        String useCredential=preferences.getString(FlowzrSyncActivity.USE_CREDENTIAL,"");
        return new FlowzrSyncOptions(useCredential,lastSyncLocalTimestamp,null);            		 		    	
    }
}
