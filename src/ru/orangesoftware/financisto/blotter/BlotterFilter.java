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

public interface BlotterFilter {

	String FROM_ACCOUNT_ID = BlotterColumns.FROM_ACCOUNT_ID;//"fromAccount.id";
	String FROM_ACCOUNT_CURRENCY_ID = BlotterColumns.FROM_ACCOUNT_CURRENCY_ID;//"fromAccount.currency.id";	
	String CATEGORY_ID = BlotterColumns.CATEGORY_ID; //"category.id";
	String CATEGORY_LEFT = BlotterColumns.CATEGORY_LEFT; //"category.left";
	String LOCATION_ID = BlotterColumns.LOCATION_ID;//"location.id";
	String PROJECT_ID = BlotterColumns.PROJECT_ID;//"project.id";
	String DATETIME = BlotterColumns.DATETIME;
	String BUDGET_ID = "budget_id";
	String IS_TEMPLATE = BlotterColumns.IS_TEMPLATE;
	
	String SORT_NEWER_TO_OLDER = BlotterColumns.DATETIME+" desc";//
	String SORT_OLDER_TO_NEWER = BlotterColumns.DATETIME+" asc";//
}
