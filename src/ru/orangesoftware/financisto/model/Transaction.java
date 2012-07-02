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
import android.content.Intent;
import android.database.Cursor;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

@Entity
@Table(name = "transactions")
public class Transaction implements Serializable, Cloneable {
	
	@Id
	@Column(name = "_id")
	public long id = -1;

    @Column(name = "parent_id")
    public long parentId;

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
    public List<Transaction> splits;

    @Transient
    public long unsplitAmount;

    public ContentValues toValues() {
		ContentValues values = new ContentValues();
        values.put(TransactionColumns.parent_id.name(), parentId);
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

    public void toIntentAsSplit(Intent intent) {
        intent.putExtra(TransactionColumns._id.name(), id);
        intent.putExtra(TransactionColumns.from_account_id.name(), fromAccountId);
        intent.putExtra(TransactionColumns.to_account_id.name(), toAccountId);
        intent.putExtra(TransactionColumns.from_amount.name(), fromAmount);
        intent.putExtra(TransactionColumns.to_amount.name(), toAmount);
        intent.putExtra(TransactionColumns.category_id.name(), categoryId);
        intent.putExtra(TransactionColumns.payee_id.name(), payeeId);
        intent.putExtra(TransactionColumns.project_id.name(), projectId);
        intent.putExtra(TransactionColumns.note.name(), note);
        intent.putExtra(TransactionColumns.last_recurrence.name(), unsplitAmount);
    }

    public static Transaction fromIntentAsSplit(Intent intent) {
        Transaction t = new Transaction();
        t.id = intent.getLongExtra(TransactionColumns._id.name(), -1);
        t.fromAccountId = intent.getLongExtra(TransactionColumns.from_account_id.name(), -1);
        t.toAccountId = intent.getLongExtra(TransactionColumns.to_account_id.name(), -1);
        t.fromAmount = intent.getLongExtra(TransactionColumns.from_amount.name(), 0);
        t.toAmount = intent.getLongExtra(TransactionColumns.to_amount.name(), 0);
        t.categoryId = intent.getLongExtra(TransactionColumns.category_id.name(), 0);
        t.payeeId = intent.getLongExtra(TransactionColumns.payee_id.name(), 0);
        t.projectId = intent.getLongExtra(TransactionColumns.project_id.name(), 0);
        t.note = intent.getStringExtra(TransactionColumns.note.name());
        t.unsplitAmount = intent.getLongExtra(TransactionColumns.last_recurrence.name(), 0);
		return t;
	}

	public static Transaction fromBlotterCursor(Cursor c) {
		long id = c.getLong(BlotterColumns._id.ordinal());
		Transaction t = new Transaction();
		t.id = id;
        t.parentId = c.getLong(BlotterColumns.parent_id.ordinal());
		t.fromAccountId = c.getLong(BlotterColumns.from_account_id.ordinal());
		t.toAccountId = c.getLong(BlotterColumns.to_account_id.ordinal());
		t.categoryId = c.getLong(BlotterColumns.category_id.ordinal());
		t.projectId = c.getLong(BlotterColumns.project_id.ordinal());
        t.payeeId = c.getLong(BlotterColumns.payee_id.ordinal());
		t.note = c.getString(BlotterColumns.note.ordinal());
		t.fromAmount = c.getLong(BlotterColumns.from_amount.ordinal());
		t.toAmount = c.getLong(BlotterColumns.to_amount.ordinal());
		t.dateTime = c.getLong(BlotterColumns.datetime.ordinal());
		t.locationId = c.getLong(BlotterColumns.location_id.ordinal());
//		t.provider = c.getString(BlotterColumns.provider.ordinal());
//		t.accuracy = c.getFloat(BlotterColumns.accuracy.ordinal());
//		t.latitude = c.getDouble(BlotterColumns.latitude.ordinal());
//		t.longitude = c.getDouble(BlotterColumns.longitude.ordinal());
		t.isTemplate = c.getInt(BlotterColumns.is_template.ordinal());
		t.templateName = c.getString(BlotterColumns.template_name.ordinal());
		t.recurrence = c.getString(BlotterColumns.recurrence.ordinal());
		t.notificationOptions = c.getString(BlotterColumns.notification_options.ordinal());
		t.status = TransactionStatus.valueOf(c.getString(BlotterColumns.status.ordinal()));
		t.attachedPicture = c.getString(BlotterColumns.attached_picture.ordinal());
		t.isCCardPayment = c.getInt(BlotterColumns.is_ccard_payment.ordinal());
		t.lastRecurrence = c.getLong(BlotterColumns.last_recurrence.ordinal());
		return t;
	}		
	
	public boolean isTransfer() {
		return toAccountId > 0;
	}

	public boolean isTemplate() {
		return isTemplate == 1;
	}

    public void setAsTemplate() {
        this.isTemplate = 1;
    }

    public boolean isScheduled() {
		return isTemplate == 2;
	}

    public void setAsScheduled() {
        this.isTemplate = 2;
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

    public boolean isSplitParent() {
        return categoryId == Category.SPLIT_CATEGORY_ID;
    }

    public boolean isSplitChild() {
        return parentId > 0;
    }

	public String getSystemAttribute(SystemAttribute sa) {
		return systemAttributes != null ? systemAttributes.get(sa) : null;
	}

    @Override
    public Transaction clone() {
        try {
            return (Transaction)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date(dateTime)).append(":");
        sb.append("FA(").append(fromAccountId).append(")->").append(fromAmount).append(",");
        sb.append("TA(").append(toAccountId).append(")->").append(toAmount);
        return sb.toString();
    }
}
