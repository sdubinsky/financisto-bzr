package ru.orangesoftware.financisto.export.qif;

import android.app.ProgressDialog;
import android.content.Context;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.ImportExportAsyncTask;
import ru.orangesoftware.financisto.export.csv.CSVExport;
import ru.orangesoftware.financisto.model.Currency;

public class QifExportTask extends ImportExportAsyncTask {

	private final WhereFilter filter;
    private final long[] selectedAccounts;

	public QifExportTask(Context context, ProgressDialog dialog, WhereFilter filter, long[] selectedAccounts) {
		super(context, dialog, null);
		this.filter = filter;
        this.selectedAccounts = selectedAccounts;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
        QifExport qifExport = new QifExport(db, filter);
        qifExport.exportOnlySelectedAccounts(selectedAccounts);
        return qifExport.export();
	}

	@Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}
