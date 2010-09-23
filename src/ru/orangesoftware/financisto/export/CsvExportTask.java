package ru.orangesoftware.financisto.export;

import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Currency;
import android.app.ProgressDialog;
import android.content.Context;

public class CsvExportTask extends ImportExportAsyncTask {
	
	private final WhereFilter filter; 
	private final Currency currency;
	private final char fieldSeparator;
	private final boolean includeHeader;
	
	public CsvExportTask(Context context, ProgressDialog dialog, WhereFilter filter, Currency currency, 
			char fieldSeparator, boolean includeHeader) {
		super(context, dialog, null);
		this.filter = filter;
		this.currency = currency;
		this.fieldSeparator = fieldSeparator;
		this.includeHeader = includeHeader;
	}
	
	@Override
	protected Object work(Context context, DatabaseAdapter db, String...params) throws Exception {
		CSVExport export = new CSVExport(db, filter, currency, fieldSeparator, includeHeader);
		return export.export();
	}

	@Override
	protected String getSuccessMessage(Object result) {
		return String.valueOf(result);
	}

}
