package ru.orangesoftware.financisto.export;

import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import android.app.ProgressDialog;
import android.content.Context;

public class BackupExportTask extends ImportExportAsyncTask {
    
    public String backupFileName;
	
	public BackupExportTask(Context context, ProgressDialog dialog) {
		super(context, dialog);
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
		DatabaseExport export = new DatabaseExport(context, db.db(), true);
        backupFileName = export.export();
        return backupFileName;
	}
	
	@Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}