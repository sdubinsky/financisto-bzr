/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.blotter;

import android.content.Context;
import android.widget.TextView;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.TransactionsTotalCalculator;
import ru.orangesoftware.financisto.model.Total;

import static ru.orangesoftware.financisto.db.DatabaseAdapter.enhanceFilterForAccountBlotter;

public class BlotterTotalCalculationTask extends TotalCalculationTask {

	private final DatabaseAdapter db;
	private final WhereFilter filter;

	public BlotterTotalCalculationTask(Context context, DatabaseAdapter db, WhereFilter filter, TextView totalText) {
        super(context, totalText);
		this.db = db;
		this.filter = filter;
	}

    @Override
    protected Total getTotal() {
        WhereFilter blotterFilter = enhanceFilterForAccountBlotter(filter);
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, blotterFilter);
        return calculator.getBlotterBalanceInHomeCurrency();
    }

}
