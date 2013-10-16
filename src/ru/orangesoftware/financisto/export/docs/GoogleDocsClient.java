/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.docs;

import android.content.Context;
import api.wireless.gdata.client.AbstructParserFactory;
import api.wireless.gdata.client.GDataParserFactory;
import api.wireless.gdata.client.ServiceDataClient;
import api.wireless.gdata.docs.client.DocsClient;
import api.wireless.gdata.docs.client.DocsGDataClient;
import api.wireless.gdata.docs.parser.xml.XmlDocsGDataParserFactory;
import api.wireless.gdata.util.AuthenticationException;
import ru.orangesoftware.financisto.backup.SettingsNotConfiguredException;
import ru.orangesoftware.financisto.utils.MyPreferences;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:19 AM
 */
public class GoogleDocsClient {

    /**
     * Connects to Google Docs
     */
    public static DocsClient createDocsClient(Context context) throws AuthenticationException, SettingsNotConfiguredException {
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
        if (login == null || login.equals(""))
            throw new SettingsNotConfiguredException("login");
        // check user password on preferences
        String password = MyPreferences.getUserPassword(context);
        if (password == null || password.equals(""))
            throw new SettingsNotConfiguredException("password");

        googleDocsClient.setUserCredentials(login, password);

        return googleDocsClient;
    }


}
