/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.docs;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import api.wireless.gdata.docs.data.DocumentEntry;
import api.wireless.gdata.parser.ParseException;
import api.wireless.gdata.util.AuthenticationException;
import api.wireless.gdata.util.ServiceException;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.backup.DatabaseImport;
import ru.orangesoftware.financisto.backup.SettingsNotConfiguredException;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;

import java.io.IOException;

import static ru.orangesoftware.financisto.export.docs.GoogleDocsClient.createDocsClient;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:16 AM
 */
public class OnlineBackupImportTask extends ImportExportAsyncTask {

    private final DocumentEntry entry;
    private final Handler handler;

    public OnlineBackupImportTask(final MainActivity mainActivity, Handler handler, ProgressDialog dialog, DocumentEntry entry) {
        super(mainActivity, dialog);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
                mainActivity.onTabChanged(mainActivity.getTabHost().getCurrentTabTag());
            }
        });
        this.entry = entry;
        this.handler = handler;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            DatabaseImport.createFromGDocsBackup(context, db, createDocsClient(context), entry).importDatabase();
        } catch (SettingsNotConfiguredException e) { // error configuring connection parameters
            if (e.getMessage().equals("login"))
                handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
            else if (e.getMessage().equals("password"))
                handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
            throw e;
        } catch (AuthenticationException e) { // authentication error
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
        return context.getString(R.string.restore_database_success);
    }

}
