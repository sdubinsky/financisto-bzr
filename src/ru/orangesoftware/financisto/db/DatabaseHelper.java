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
package ru.orangesoftware.financisto.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;

import static ru.orangesoftware.financisto.utils.EnumUtils.asStringArray;

public class DatabaseHelper extends DatabaseSchemaEvolution {
	
	public DatabaseHelper(Context context) {
		super(context, Database.DATABASE_NAME, null, Database.DATABASE_VERSION);
		setAutoDropViews(true);
	}
	
	public void forceRunAlterScript(SQLiteDatabase db, String name) 
		throws IOException {
		runAlterScript(db, name);
	}
	
	@Override
	protected String getViewNameFromScriptName(String scriptFileName) {
		int x = scriptFileName.indexOf('[');
		int y = scriptFileName.indexOf(']');
		if (x != -1 && y != -1 && y - x > 1) {
			return scriptFileName.substring(x+1, y);	
		}
		return null;
	}

	public static final String TRANSACTION_TABLE = "transactions";		
	public static final String ACCOUNT_TABLE = "account";
	public static final String CURRENCY_TABLE = "currency";
	public static final String CATEGORY_TABLE = "category";
	public static final String BUDGET_TABLE = "budget";
	public static final String PROJECT_TABLE = "project";
	public static final String ATTRIBUTES_TABLE = "attributes";
	public static final String CATEGORY_ATTRIBUTE_TABLE = "category_attribute";
	public static final String TRANSACTION_ATTRIBUTE_TABLE = "transaction_attribute";
	public static final String LOCATIONS_TABLE = "locations";
    public static final String PAYEE_TABLE = "payee";

	public static final String V_ALL_TRANSACTIONS = "v_all_transactions";
	public static final String V_BLOTTER = "v_blotter";
	public static final String V_BLOTTER_FOR_ACCOUNT = "v_blotter_for_account";
	public static final String V_ACCOUNT = "v_account";
	public static final String V_CATEGORY = "v_category";
	public static final String V_BUDGET = "v_budget";
	public static final String V_ATTRIBUTES = "v_attributes";
	public static final String V_REPORT_CATEGORY = "v_report_category";
	public static final String V_REPORT_SUB_CATEGORY = "v_report_sub_category";	
	public static final String V_REPORT_PERIOD = "v_report_period";
	public static final String V_REPORT_LOCATIONS = "v_report_location";	
	public static final String V_REPORT_PROJECTS = "v_report_project";
    public static final String V_REPORT_PAYEES = "v_report_payee";
	
	public static enum TransactionColumns {
        _id,
        from_account_id,
        to_account_id,
        category_id,
        project_id,
        payee_id,
        note,
        from_amount,
        to_amount,
        datetime,
        location_id,
        provider,
        accuracy,
        latitude,
        longitude,
        is_template,
        template_name,
        recurrence,
        notification_options,
        status,
        attached_picture,
        is_ccard_payment,
        last_recurrence;
		
		public static String[] NORMAL_PROJECTION = asStringArray(TransactionColumns.values());

	}
	
	public static enum BlotterColumns {
        _id,
        from_account_id,
        from_account_title,
        from_account_currency_id,
        to_account_id,
        to_account_title,
        to_account_currency_id,
        category_id,
        category_title,
        category_left,
        category_right,
        project_id,
        project,
        location_id,
        location,
        payee_id,
        payee,
		note,
		from_amount,
		to_amount,
		datetime,
		is_template,
		template_name,
		recurrence,
		notification_options,
		status;

		public static final String[] NORMAL_PROJECTION = asStringArray(BlotterColumns.values());

        public static final String[] BALANCE_PROJECTION = {
			from_account_currency_id.name(),
			"SUM("+from_amount+")"
		};

		public static final String BALANCE_GROUP_BY = "FROM_ACCOUNT_CURRENCY_ID";
	}

    public static class AccountColumns {
		
		public static final String ID = "_id";
		public static final String TITLE = "title";
		public static final String CREATION_DATE = "creation_date";		
		public static final String CURRENCY_ID = "currency_id";
		public static final String TYPE = "type";
		public static final String ISSUER = "issuer";
		public static final String TOTAL_AMOUNT = "total_amount";
		public static final String SORT_ORDER = "sort_order";
		public static final String LAST_CATEGORY_ID = "last_category_id";
		public static final String LAST_ACCOUNT_ID = "last_account_id";
		public static final String CLOSING_DAY = "closing_day";
		public static final String PAYMENT_DAY = "payment_day";
		
		private AccountColumns() {}

	}
	
	public static class CategoryColumns {
		public static final String ID = "_id";		
		public static final String TITLE = "title";		
		public static final String LEFT = "left";
		public static final String RIGHT = "right";		
		public static final String LAST_LOCATION_ID = "last_location_id";
		public static final String LAST_PROJECT_ID = "last_project_id";
		public static final String SORT_ORDER = "sort_order";

		public static class Indicies {
			public static final int ID = 0;
			public static final int TITLE = 1;
			public static final int LEFT = 2;
			public static final int RIGHT = 3;
		}
	}

	public static class CategoryViewColumns extends CategoryColumns {
		public static final String LEVEL = "level";
		
		public static String[] NORMAL_PROJECTION = {
			ID,
			TITLE,
			LEVEL,
			LEFT,
			RIGHT,
			LAST_LOCATION_ID,
			LAST_PROJECT_ID,
			SORT_ORDER
		};
		
		public static class Indicies {
			public static final int ID = 0;
			public static final int TITLE = 1;		
			public static final int LEVEL = 2;
			public static final int LEFT = 3;
			public static final int RIGHT = 4;			
			public static final int LAST_LOCATION_ID = 5;
			public static final int LAST_PROJECT_ID = 6;
			public static final int SORT_ORDER = 7;
		}
	}

	public static class EntityColumns {
		
		public static final String ID = "_id";
		public static final String TITLE = "title";
		
		public static final String[] NORMAL_PROJECTION = {
			ID,
			TITLE
		};
		
		public static class Indicies {
			public static final int ID = 0;
			public static final int TITLE = 1;						
		}
		
	}

	public static class AttributeColumns {
		
		public static final String ID = "_id";
		public static final String NAME = "name";
		public static final String TYPE = "type";
		public static final String LIST_VALUES = "list_values";
		public static final String DEFAULT_VALUE = "default_value";
		
		public static final String[] NORMAL_PROJECTION = {
			ID,
			NAME,
			TYPE,
			LIST_VALUES,
			DEFAULT_VALUE
		};
		
		public static class Indicies {
			public static final int ID = 0;
			public static final int NAME = 1;
			public static final int TYPE = 2;
			public static final int LIST_VALUES = 3;
			public static final int DEFAULT_VALUE = 4;
		}
		
	}
	
	public static class AttributeViewColumns {
		
		public static final String NAME = "name";
		public static final String CATEGORY_ID = "category_id";
		public static final String CATEGORY_LEFT = "category_left";
		public static final String CATEGORY_RIGHT = "category_right";

		public static final String[] NORMAL_PROJECTION = {
			CATEGORY_ID,
			NAME
		};
		
		public static class Indicies {
			public static final int CATEGORY_ID = 0;
			public static final int NAME = 1;
		}
	}

	public static class CategoryAttributeColumns {
		public static final String ATTRIBUTE_ID = "attribute_id";		
		public static final String CATEGORY_ID = "category_id";
	}

	public static class TransactionAttributeColumns {
		public static final String ATTRIBUTE_ID = "attribute_id";
		public static final String TRANSACTION_ID = "transaction_id";		
		public static final String VALUE = "value";
		
		public static final String[] NORMAL_PROJECTION = {
			ATTRIBUTE_ID,
			TRANSACTION_ID,
			VALUE
		};
		
		public static class Indicies {
			public static final int ATTRIBUTE_ID = 0;
			public static final int TRANSACTION_ID = 1;
			public static final int VALUE = 2;
		}
		
	}

	public static class ReportColumns {
		
		public static final String ID = "_id";
		public static final String NAME = "name";	
		public static final String CURRENCY_ID = "currency_id";
		public static final String AMOUNT = "amount";
		public static final String DATETIME = "datetime";
		public static final String IS_TRANSFER = "is_transfer";
		
		public static String[] NORMAL_PROJECTION = {ID, NAME, CURRENCY_ID, AMOUNT, DATETIME, IS_TRANSFER};
		
		public static interface Indicies {
			public static final int ID = 0;
			public static final int NAME = 1; 
			public static final int CURRENCY_ID = 2; 
			public static final int AMOUNT = 3;
			public static final int DATETIME = 4;
			public static final int IS_TRANSFER = 5;
		}
	}

	public static class SubCategoryReportColumns extends ReportColumns {

		public static final String LEFT = "left";
		public static final String RIGHT = "right";

		public static String[] NORMAL_PROJECTION = {ID, NAME, CURRENCY_ID, AMOUNT, LEFT, RIGHT};		
	}
	
	public static class LocationColumns {
		
		public static final String ID = "_id";
		public static final String NAME = "name";	
		public static final String DATETIME = "datetime";
		public static final String PROVIDER = "provider";
		public static final String ACCURACY = "accuracy";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String IS_PAYEE = "is_payee";
		public static final String RESOLVED_ADDRESS = "resolved_address";
		
		public static String[] NORMAL_PROJECTION = {ID, NAME, DATETIME, PROVIDER, ACCURACY, LATITUDE, LONGITUDE, IS_PAYEE, RESOLVED_ADDRESS};		
	}
	
}
