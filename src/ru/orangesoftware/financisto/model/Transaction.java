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

import android.content.ContentValues;
import android.database.Cursor;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;

import javax.persistence.*;
import java.util.EnumMap;
import java.util.List;

@Entity
@Table(name = "transactions")
public class Transaction {
	
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
	
    @Column(name = "payee_id")
    public long payeeId;

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

	@Column(name = "last_recurrence")
	public long lastRecurrence;

	@Transient
	public EnumMap<SystemAttribute, String> systemAttributes;

    @Transient
    public List<Split> splits;

    public ContentValues toValues() {
		ContentValues values = new ContentValues();
		values.put(TransactionColumns.category_id.name(), categoryId);
		values.put(TransactionColumns.project_id.name(), projectId);
		values.put(TransactionColumns.datetime.name(), dateTime);
		values.put(TransactionColumns.location_id.name(), locationId);
		values.put(TransactionColumns.provider.name(), provider);
		values.put(TransactionColumns.accuracy.name(), accuracy);
		values.put(TransactionColumns.latitude.name(), latitude);
		values.put(TransactionColumns.longitude.name(), longitude);
		values.put(TransactionColumns.from_account_id.name(), fromAccountId);
		values.put(TransactionColumns.to_account_id.name(), toAccountId);
        values.put(TransactionColumns.payee_id.name(), payeeId);
		values.put(TransactionColumns.note.name(), note);
		values.put(TransactionColumns.from_amount.name(), fromAmount);
		values.put(TransactionColumns.to_amount.name(), toAmount);
		values.put(TransactionColumns.is_template.name(), isTemplate);
		values.put(TransactionColumns.template_name.name(), templateName);
		values.put(TransactionColumns.recurrence.name(), recurrence);
		values.put(TransactionColumns.notification_options.name(), notificationOptions);
		values.put(TransactionColumns.status.name(), status.name());
		values.put(TransactionColumns.attached_picture.name(), attachedPicture);
		values.put(TransactionColumns.is_ccard_payment.name(), isCCardPayment);
		values.put(TransactionColumns.last_recurrence.name(), lastRecurrence);
		return values;
	}

	public static Transaction fromCursor(Cursor c) {
		long id = c.getLong(TransactionColumns._id.ordinal());
		Transaction t = new Transaction();
		t.id = id;
		t.fromAccountId = c.getLong(TransactionColumns.from_account_id.ordinal());
		t.toAccountId = c.getLong(TransactionColumns.to_account_id.ordinal());
		t.categoryId = c.getLong(TransactionColumns.category_id.ordinal());
		t.projectId = c.getLong(TransactionColumns.project_id.ordinal());
        t.payeeId = c.getLong(TransactionColumns.payee_id.ordinal());
		t.note = c.getString(TransactionColumns.note.ordinal());
		t.fromAmount = c.getLong(TransactionColumns.from_amount.ordinal());
		t.toAmount = c.getLong(TransactionColumns.to_amount.ordinal());
		t.dateTime = c.getLong(TransactionColumns.datetime.ordinal());
		t.locationId = c.getLong(TransactionColumns.location_id.ordinal());
		t.provider = c.getString(TransactionColumns.provider.ordinal());
		t.accuracy = c.getFloat(TransactionColumns.accuracy.ordinal());
		t.latitude = c.getDouble(TransactionColumns.latitude.ordinal());
		t.longitude = c.getDouble(TransactionColumns.longitude.ordinal());
		t.isTemplate = c.getInt(TransactionColumns.is_template.ordinal());
		t.templateName = c.getString(TransactionColumns.template_name.ordinal());
		t.recurrence = c.getString(TransactionColumns.recurrence.ordinal());
		t.notificationOptions = c.getString(TransactionColumns.notification_options.ordinal());
		t.status = TransactionStatus.valueOf(c.getString(TransactionColumns.status.ordinal()));
		t.attachedPicture = c.getString(TransactionColumns.attached_picture.ordinal());
		t.isCCardPayment = c.getInt(TransactionColumns.is_ccard_payment.ordinal());
		t.lastRecurrence = c.getLong(TransactionColumns.last_recurrence.ordinal());
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
