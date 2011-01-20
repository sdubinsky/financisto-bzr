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
package ru.orangesoftware.financisto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractListActivity;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.info.TransactionInfo;
import ru.orangesoftware.financisto.recur.Recurrence;
import ru.orangesoftware.financisto.utils.ThumbnailUtil;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.NodeInflater;

import java.util.List;

import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;

public class TransactionInfoDialog {

	private final AbstractListActivity parentActivity;
	private final int position;
	private final long id;
	private final MyEntityManager em;
	private final NodeInflater inflater;
	private final LayoutInflater layoutInflater;	
	
	public TransactionInfoDialog(AbstractListActivity parentActivity, int position, long id, 
			MyEntityManager em, NodeInflater inflater) {
		this.parentActivity = parentActivity;
		this.position = position;
		this.id = id;
		this.em = em;
		this.inflater = inflater;
		this.layoutInflater = (LayoutInflater)parentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void show(long transactionId) {
		final AbstractListActivity parentActivity = this.parentActivity;
		View v = layoutInflater.inflate(R.layout.transaction_info, null);
		LinearLayout layout = (LinearLayout)v.findViewById(R.id.list);
		TransactionInfo ti = em.getTransactionInfo(transactionId);
		if (ti == null) {
			Toast t = Toast.makeText(parentActivity, R.string.no_transaction_found, Toast.LENGTH_LONG);
			t.show();
			return;
		}

        TransactionStatus status = TransactionStatus.valueOf(ti.status);
        View titleView = layoutInflater.inflate(R.layout.transaction_info_title, null);
        TextView titleLabel = (TextView)titleView.findViewById(R.id.label);
        TextView titleData = (TextView)titleView.findViewById(R.id.data);
        ImageView titleIcon = (ImageView)titleView.findViewById(R.id.icon);
		if (ti.isTemplate()) {
            titleLabel.setText(ti.templateName);
		} else {
			if (ti.isSchedule() && ti.recurrence != null) {
				Recurrence r = Recurrence.parse(ti.recurrence);
                titleLabel.setText(r.toInfoString(parentActivity));
			} else {
                int titleId = ti.toAccount == null ? R.string.transaction : R.string.transfer;
                titleLabel.setText(titleId);
				add(layout, R.string.date, DateUtils.formatDateTime(parentActivity, ti.dateTime,
						DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_YEAR),
						ti.attachedPicture);				
			}
		}
        titleData.setText(parentActivity.getString(status.titleId));
        titleIcon.setImageResource(status.iconId);
		if (ti.toAccount == null) {
			AccountType formAccountType = AccountType.valueOf(ti.fromAccount.type);
			add(layout, R.string.account, ti.fromAccount.title, formAccountType);
            if (ti.payee != null) {
                add(layout, R.string.payee, ti.payee.title);
            }
            add(layout, R.string.category, ti.category.title);
			add(layout, R.string.amount, Utils.amountToString(ti.fromAccount.currency, ti.fromAmount));
		} else {
			AccountType fromAccountType = AccountType.valueOf(ti.fromAccount.type);
			add(layout, R.string.account_from, ti.fromAccount.title, fromAccountType);
			add(layout, R.string.amount_from, Utils.amountToString(ti.fromAccount.currency, ti.fromAmount));
			AccountType toAccountType = AccountType.valueOf(ti.toAccount.type);
			add(layout, R.string.account_to, ti.toAccount.title, toAccountType);
			add(layout, R.string.amount_to, Utils.amountToString(ti.toAccount.currency, ti.toAmount));
            add(layout, R.string.category, ti.category.title);
		}
		List<TransactionAttributeInfo> attributes = em.getAttributesForTransaction(transactionId);
		for (TransactionAttributeInfo tai : attributes) {
            String value = tai.getValue(parentActivity);
            if (isNotEmpty(value)) {
			    add(layout, tai.name, value);
            }
		}

        Project project = ti.project;
        if (project != null && project.id > 0) {
            add(layout, R.string.project, project.title);
        }

        if (!Utils.isEmpty(ti.note)) {
            add(layout, R.string.note, ti.note);
        }

        MyLocation location = ti.location;
        String locationName;
        if (location != null && location.id > 0) {
            locationName = location.name+(location.resolvedAddress != null ? " ("+location.resolvedAddress+")" : "");
            add(layout, R.string.location, locationName);
        }

		final Dialog d = new AlertDialog.Builder(parentActivity)
			.setCustomTitle(titleView)
			.setView(v)
			.create();
		d.setCanceledOnTouchOutside(true);

		Button bEdit = (Button)v.findViewById(R.id.bEdit);
		bEdit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				d.dismiss();
				parentActivity.editItem(position, id);
			}
		});
		
		Button bClose = (Button)v.findViewById(R.id.bClose);
		bClose.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				d.dismiss();
			}
		});

		d.show();
	}

	private void add(LinearLayout layout, int labelId, String data, AccountType accountType) {
		inflater.new Builder(layout, R.layout.select_entry_simple_icon)
			.withIcon(accountType.iconId).withLabel(labelId).withData(data).create();
	}

	private void add(LinearLayout layout, int labelId, String data) {
		inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(labelId)
			.withData(data).create();
	}

	private void add(LinearLayout layout, int labelId, String data, String pictureFileName) {
		Bitmap thumb = ThumbnailUtil.loadThumbnail(pictureFileName);
		View v = inflater.new PictureBuilder(layout)
			.withPicture(parentActivity, thumb)
			.withLabel(labelId)
			.withData(data).create();
		v.setClickable(false);
		v.setFocusable(false);
		v.setFocusableInTouchMode(false);
		ImageView pictureView = (ImageView)v.findViewById(R.id.picture);
		pictureView.setTag(pictureFileName);
	}

	private void add(LinearLayout layout, String label, String data) {
		inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(label)
			.withData(data).create();
	}

}
