/*
 * Copyright (c) 2014 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export.docs;

import android.app.Dialog;
import android.os.AsyncTask;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MainActivity;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.csv.Csv;
import ru.orangesoftware.financisto.utils.MyPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 1/20/14
 * Time: 11:58 PM
 */
public class ListGoogleDriveFilesTask extends AsyncTask<Void, Void, File[]> {

    private final MainActivity context;
    private final Dialog dialog;

    private volatile int error = 0;

    public ListGoogleDriveFilesTask(MainActivity context, Dialog dialog) {
        this.context = context;
        this.dialog = dialog;
    }

    @Override
    protected File[] doInBackground(Void... contexts) {
        try {
            Drive drive = GoogleDriveClient.create(context);

            if (drive == null) {
                error = R.string.google_drive_permission_required;
                return null;
            }

            // get the list of files in the repository
            String targetFolder = MyPreferences.getBackupFolder(context);
            // check the backup folder registered on preferences
            if (targetFolder == null || targetFolder.equals("")) {
                error = R.string.gdocs_folder_not_configured;
                return null;
            }

            String folderId = GoogleDriveClient.getOrCreateDriveFolder(drive, targetFolder);

            List<File> backupFiles = new ArrayList<File>();
            FileList files = drive.files().list().setQ("mimeType='" + Export.BACKUP_MIME_TYPE + "' and '" + folderId + "' in parents").execute();
            for (com.google.api.services.drive.model.File f : files.getItems()) {
                if ((f.getExplicitlyTrashed() == null || !f.getExplicitlyTrashed()) && f.getDownloadUrl() != null && f.getDownloadUrl().length() > 0) {
                    if (f.getFileExtension().equals("backup")) {
                        backupFiles.add(f);
                    }
                }
            }
            return backupFiles.toArray(new File[backupFiles.size()]);

        } catch (Csv.IOException e) {
            error = R.string.gdocs_io_error;
            return null;
        } catch (Exception e) {
            error = R.string.gdocs_connection_failed;
            return null;
        }
    }

    @Override
    protected void onPostExecute(File[] backupFiles) {
        dialog.dismiss();
        if (error != 0) {
            context.showErrorPopup(context, error);
            return;
        }
        context.doImportFromGoogleDrive(backupFiles);
    }


}
