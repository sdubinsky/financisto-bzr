/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.blotter;

import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;

import static ru.orangesoftware.financisto.utils.EnumUtils.lower;

public interface BlotterFilter {

	String FROM_ACCOUNT_ID = lower(BlotterColumns.FROM_ACCOUNT_ID);
	String FROM_ACCOUNT_CURRENCY_ID = lower(BlotterColumns.FROM_ACCOUNT_CURRENCY_ID);
	String CATEGORY_ID = lower(BlotterColumns.CATEGORY_ID);
	String CATEGORY_LEFT = lower(BlotterColumns.CATEGORY_LEFT);
	String LOCATION_ID = lower(BlotterColumns.LOCATION_ID);
	String PROJECT_ID = lower(BlotterColumns.PROJECT_ID);
    String PAYEE_ID = lower(BlotterColumns.PAYEE_ID);
	String DATETIME = lower(BlotterColumns.DATETIME);
	String BUDGET_ID = "budget_id";
	String IS_TEMPLATE = lower(BlotterColumns.IS_TEMPLATE);
	String STATUS = lower(BlotterColumns.STATUS);
	
	String SORT_NEWER_TO_OLDER = BlotterColumns.DATETIME+" desc";
	String SORT_OLDER_TO_NEWER = BlotterColumns.DATETIME+" asc";

}
