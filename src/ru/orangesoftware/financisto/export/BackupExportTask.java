package ru.orangesoftware.financisto.export;

import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import android.app.ProgressDialog;
import android.content.Context;

public class BackupExportTask extends ImportExportAsyncTask {
	
	public BackupExportTask(Context context, ProgressDialog dialog) {
		super(context, dialog, null);
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
		DatabaseExport export = new DatabaseExport(context, db.db());
		return export.export();

	}
	
	@Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}