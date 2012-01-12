package ru.orangesoftware.financisto.export;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import ru.orangesoftware.financisto.utils.MyPreferences;

import static ru.orangesoftware.financisto.export.Export.uploadBackupFileToDropbox;

public class BackupExportTask extends ImportExportAsyncTask {

    public final boolean uploadToDropbox;

    public volatile String backupFileName;
	
	public BackupExportTask(Context context, ProgressDialog dialog, boolean uploadToDropbox) {
		super(context, dialog);
        this.uploadToDropbox = uploadToDropbox;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
		DatabaseExport export = new DatabaseExport(context, db.db(), true);
        backupFileName = export.export();
        if (uploadToDropbox && MyPreferences.isDropboxUploadBackups(context)) {
            publishProgress(context.getString(R.string.dropbox_uploading_file));
            uploadBackupFileToDropbox(context, backupFileName);
        }
        return backupFileName;
	}

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        dialog.setMessage(values[0]);
    }

    @Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}