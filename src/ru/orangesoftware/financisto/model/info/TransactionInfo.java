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
package ru.orangesoftware.financisto.model.info;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.TransactionActivity;
import ru.orangesoftware.financisto.activity.TransferActivity;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.Utils;
import android.app.Activity;
import android.content.Context;

@Entity
@Table(name = "transactions")
public class TransactionInfo {
	
	@Id
	@Column(name = "_id")
	public long id = -1;
	
	@JoinColumn(name = "from_account_id")
	public Account fromAccount;

	@JoinColumn(name = "to_account_id", required = false)
	public Account toAccount;

	@JoinColumn(name = "category_id")
	public Category category;

	@JoinColumn(name = "project_id", required = false)
	public Project project;

	@JoinColumn(name = "location_id", required = false)
	public MyLocation location;

	@Column(name = "from_amount")
	public long fromAmount;

	@Column(name = "to_amount")
	public long toAmount;

	@Column(name = "datetime")
	public long dateTime;
	
    @JoinColumn(name = "payee_id", required = false)
    public Payee payee;

	@Column(name = "note")
	public String note;
	
	@Column(name = "provider")
	public String provider;
	
	@Column(name = "accuracy")
	public Float accuracy;
	
	@Column(name = "latitude")
	public Double latitude;
	
	@Column(name = "longitude")
	public Double longitude;

	@Column(name = "is_template")
	public int isTemplate;

	@Column(name = "template_name")
	public String templateName;

	@Column(name = "recurrence")
	public String recurrence;

	@Column(name = "notification_options")
	public String notificationOptions;		

	@Column(name = "status")
	public String status;		

	@Column(name = "attached_picture")
	public String attachedPicture;		

	@Column(name = "last_recurrence")
	public long lastRecurrence;		

	@Transient
	public Date nextDateTime;
	
	public boolean isTemplate() {
		return isTemplate == 1;
	}

	public boolean isSchedule() {
		return isTemplate == 2;
	}

	private boolean isTransfer() {
		return toAccount != null;
	}

	public Class<? extends Activity> getActivity() {
		return isTransfer() ? TransferActivity.class : TransactionActivity.class;
	}
	
	public int getNotificationIcon() {
		return isTransfer() ? R.drawable.notification_icon_transfer : R.drawable.notification_icon_transaction;
	}
	
	public String getNotificationTickerText(Context context) {
		return context.getString(isTransfer() ? R.string.new_scheduled_transfer_text : R.string.new_scheduled_transaction_text);
	}

	public String getNotificationContentTitle(Context context) {
		return context.getString(isTransfer() ? R.string.new_scheduled_transfer_title : R.string.new_scheduled_transaction_title);
	}
	
	public String getNotificationContentText(Context context) {
		if (toAccount != null) {
			if (fromAccount.currency.id == toAccount.currency.id) {
				return context.getString(R.string.new_scheduled_transfer_notification_same_currency, 
						Utils.amountToString(fromAccount.currency, Math.abs(fromAmount)),
						fromAccount.title, toAccount.title);				
			} else {
				return context.getString(R.string.new_scheduled_transfer_notification_differ_currency, 
						Utils.amountToString(fromAccount.currency, Math.abs(fromAmount)),
						Utils.amountToString(toAccount.currency, Math.abs(toAmount)),
						fromAccount.title, toAccount.title);								
			}
		} else {
			return context.getString(R.string.new_scheduled_transaction_notification, 
					Utils.amountToString(fromAccount.currency, Math.abs(fromAmount)),
					context.getString(fromAmount < 0 ? R.string.new_scheduled_transaction_debit : R.string.new_scheduled_transaction_credit),
					fromAccount.title);
		}		
	}

}
