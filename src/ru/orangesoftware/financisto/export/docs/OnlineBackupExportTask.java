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
import android.content.pm.PackageManager;
import android.os.Handler;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.services.drive.Drive;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.backup.SettingsNotConfiguredException;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.io.IOException;

import static ru.orangesoftware.financisto.export.docs.GoogleDriveClient.create;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:23 AM
 */
public class OnlineBackupExportTask extends ImportExportAsyncTask {

    private final Handler handler;

    public OnlineBackupExportTask(MainActivity mainActivity, Handler handler, ProgressDialog dialog) {
        super(mainActivity, dialog);
        this.handler = handler;
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        try {
            String folder = MyPreferences.getBackupFolder(context);
            // check the backup folder registered on preferences
            if (folder == null || folder.equals("")) {
                throw new SettingsNotConfiguredException("folder-is-null");
            }
            Drive drive = create(context);
            if (drive == null) {
                throw new SettingsNotConfiguredException("drive-is-null");
            }
            return export.exportOnline(drive, folder);
        } catch (GoogleAuthException e) { // connection error
            handler.sendEmptyMessage(R.string.gdocs_connection_failed);
            throw e;
        } catch (SettingsNotConfiguredException e) { // missing login or password
            if (e.getMessage().equals("folder-is-null"))
                handler.sendEmptyMessage(R.string.gdocs_folder_not_configured);
            else if (e.getMessage().equals("folder-not-found"))
                handler.sendEmptyMessage(R.string.gdocs_folder_not_found);
            else if (e.getMessage().equals("drive-is-null"))
                handler.sendEmptyMessage(R.string.google_drive_permission_required);
            throw e;
        } catch (PackageManager.NameNotFoundException e) {
            handler.sendEmptyMessage(R.string.package_info_error);
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
