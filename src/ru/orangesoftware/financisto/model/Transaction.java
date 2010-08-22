/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - identifying credit card payments
 ******************************************************************************/
package ru.orangesoftware.financisto.model;

import java.util.EnumMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import android.content.ContentValues;
import android.database.Cursor;

@Entity
@Table(name = "transactions")
public class Transaction {
	
	public static int TRANSFER_IN = 1;
	public static int TRANSFER_OUT = 2;
	
	@Id
	@Column(name = "_id")
	public long id = -1;

	@Column(name = "category_id")
	public long categoryId;
	
	@Column(name = "project_id")
	public long projectId;
	
	@Column(name = "datetime")
	public long dateTime = System.currentTimeMillis();
	
	@Column(name = "location_id")
	public long locationId;
	
	@Column(name = "provider")
	public String provider;
	
	@Column(name = "accuracy")
	public float accuracy;
	
	@Column(name = "longitude")
	public double longitude;
	
	@Column(name = "latitude")
	public double latitude;
	
	@Column(name = "from_account_id")
	public long fromAccountId;
	
	@Column(name = "to_account_id")
	public long toAccountId;
	
	@Column(name = "note")
	public String note;
	
	@Column(name = "from_amount")
	public long fromAmount;
	
	@Column(name = "to_amount")
	public long toAmount;
	
	@Column(name = "is_template")
	public int isTemplate;
	
	@Column(name = "template_name")
	public String templateName;

	@Column(name = "recurrence")
	public String recurrence;	
	
	@Column(name = "notification_options")
	public String notificationOptions;		
	
	@Column(name = "status")
	public TransactionStatus status = TransactionStatus.UR;	
	
	@Column(name = "attached_picture")
	public String attachedPicture;
	
	@Column(name = "is_ccard_payment")
	public int isCCardPayment;

	@Transient
	public EnumMap<SystemAttribute, String> systemAttributes;

	public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(TransactionColumns.CATEGORY_ID, categoryId);
		values.put(TransactionColumns.PROJECT_ID, projectId);
		values.put(TransactionColumns.DATETIME, dateTime);
		values.put(TransactionColumns.LOCATION_ID, locationId);
		values.put(TransactionColumns.PROVIDER, provider);
		values.put(TransactionColumns.ACCURACY, accuracy);
		values.put(TransactionColumns.LATITUDE, latitude);
		values.put(TransactionColumns.LONGITUDE, longitude);
		values.put(TransactionColumns.FROM_ACCOUNT_ID, fromAccountId);
		values.put(TransactionColumns.TO_ACCOUNT_ID, toAccountId);
		values.put(TransactionColumns.NOTE, note);
		values.put(TransactionColumns.FROM_AMOUNT, fromAmount);
		values.put(TransactionColumns.TO_AMOUNT, toAmount);
		values.put(TransactionColumns.IS_TEMPLATE, isTemplate);
		values.put(TransactionColumns.TEMPLATE_NAME, templateName);
		values.put(TransactionColumns.RECURRENCE, recurrence);
		values.put(TransactionColumns.NOTIFICATION_OPTIONS, notificationOptions);
		values.put(TransactionColumns.STATUS, status.name());
		values.put(TransactionColumns.ATTACHED_PICTURE, attachedPicture);
		values.put(TransactionColumns.IS_CCARD_PAYMENT, isCCardPayment);
		return values;
	}

	public static Transaction fromCursor(Cursor c) {
		long id = c.getLong(TransactionColumns.Indicies.ID);
		Transaction t = new Transaction();
		t.id = id;
		t.fromAccountId = c.getLong(TransactionColumns.Indicies.FROM_ACCOUNT_ID);
		t.toAccountId = c.getLong(TransactionColumns.Indicies.TO_ACCOUNT_ID);
		t.categoryId = c.getLong(TransactionColumns.Indicies.CATEGORY_ID);
		t.projectId = c.getLong(TransactionColumns.Indicies.PROJECT_ID);
		t.note = c.getString(TransactionColumns.Indicies.NOTE);
		t.fromAmount = c.getLong(TransactionColumns.Indicies.FROM_AMOUNT);
		t.toAmount = c.getLong(TransactionColumns.Indicies.TO_AMOUNT);
		t.dateTime = c.getLong(TransactionColumns.Indicies.DATETIME);
		t.locationId = c.getLong(TransactionColumns.Indicies.LOCATION_ID);
		t.provider = c.getString(TransactionColumns.Indicies.PROVIDER);
		t.accuracy = c.getFloat(TransactionColumns.Indicies.ACCURACY);
		t.latitude = c.getDouble(TransactionColumns.Indicies.LATITUDE);
		t.longitude = c.getDouble(TransactionColumns.Indicies.LONGITUDE);
		t.isTemplate = c.getInt(TransactionColumns.Indicies.IS_TEMPLATE);
		t.templateName = c.getString(TransactionColumns.Indicies.TEMPLATE_NAME);
		t.recurrence = c.getString(TransactionColumns.Indicies.RECURRENCE);
		t.notificationOptions = c.getString(TransactionColumns.Indicies.NOTIFICATION_OPTIONS);	
		t.status = TransactionStatus.valueOf(c.getString(TransactionColumns.Indicies.STATUS));
		t.attachedPicture = c.getString(TransactionColumns.Indicies.ATTACHED_PICTURE);
		t.isCCardPayment = c.getInt(TransactionColumns.Indicies.IS_CCARD_PAYMENT);
		return t;
	}		
	
	public boolean isTransfer() {
		return toAccountId > 0;
	}
	
	public boolean isTemplate() {
		return isTemplate == 1;
	}
	
	public boolean isScheduled() {
		return isTemplate == 2;
	}

	public boolean isTemplateLike() {
		return isTemplate > 0;
	}

	public boolean isNotTemplateLike() {
		return isTemplate == 0;
	}
	
	public boolean isCreditCardPayment() {
		return isCCardPayment == 1;
	}
	
	public String getSystemAttribute(SystemAttribute sa) {
		return systemAttributes != null ? systemAttributes.get(sa) : null;
	}
	
}
