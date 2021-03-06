/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.dropbox;

import android.content.Context;
import android.util.Log;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/10/12 10:52 PM
 */
public class Dropbox {

    public static final String APP_KEY = "INSERT_APP_KEY_HERE";
    public static final String APP_SECRET = "INSERT_APP_SECRET_HERE";
    public static final Session.AccessType ACCESS_TYPE = Session.AccessType.APP_FOLDER;

    private final Context context;
    private final DropboxAPI<AndroidAuthSession> dropboxApi;

    private boolean startedAuth = false;

    public Dropbox(Context context) {
        this.context = context;
        this.dropboxApi = createApi();
    }

    public void startAuth() {
        startedAuth = true;
        dropboxApi.getSession().startAuthentication(context);
    }

    public void completeAuth() {
        try {
            if (startedAuth && dropboxApi.getSession().authenticationSuccessful()) {
                try {
                    dropboxApi.getSession().finishAuthentication();
                    AccessTokenPair tokens = dropboxApi.getSession().getAccessTokenPair();
                    MyPreferences.storeDropboxKeys(context, tokens.key, tokens.secret);
                } catch (IllegalStateException e) {
                    Log.i("Financisto", "Error authenticating Dropbox", e);
                }
            }
        } finally {
            startedAuth = false;
        }
    }

    public void deAuth() {
        MyPreferences.removeDropboxKeys(context);
        dropboxApi.getSession().unlink();
    }

    private DropboxAPI<AndroidAuthSession> createApi() {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
        return new DropboxAPI<AndroidAuthSession>(session);
    }

    public boolean authSession() {
        AccessTokenPair access = MyPreferences.getDropboxKeys(context);
        if (access != null) {
            dropboxApi.getSession().setAccessTokenPair(access);
            return dropboxApi.getSession().isLinked();
        }
        return false;
    }

    public void uploadFile(File file) {
        if (authSession()) {
            try {
                InputStream is = new FileInputStream(file);
                DropboxAPI.Entry newEntry = dropboxApi.putFile(file.getName(), is, file.length(), null, null);
                Log.i("Financisto", "Dropbox: The uploaded file's rev is: " + newEntry.rev);
            } catch (DropboxException e) {
                Log.e("Financisto", "Dropbox: Something wrong", e);
            } catch (Exception e) {
                Log.e("Financisto", "Dropbox: Something wrong with uploading", e);
            }
        }
    }
    
}
