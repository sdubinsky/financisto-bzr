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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.backup.DatabaseImport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:16 AM
 */
public class OnlineBackupImportTask extends ImportExportAsyncTask {

    private final com.google.api.services.drive.model.File entry;
    private final Handler handler;

    public OnlineBackupImportTask(final MainActivity mainActivity, Handler handler, ProgressDialog dialog, File entry) {
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
            Drive drive = GoogleDriveClient.create(context);
            DatabaseImport.createFromGDocsBackup(context, db, drive, entry).importDatabase();
        } catch (IOException e) {
            handler.sendEmptyMessage(R.string.gdocs_io_error);
            throw e;
        } catch (Exception e) {
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
