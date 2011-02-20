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
package ru.orangesoftware.financisto.adapter;

import java.text.DateFormat;
import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.CardIssuer;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.orb.EntityManager;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class AccountListAdapter extends ResourceCursorAdapter {
	
	private final Utils u;
	private DateFormat df;
	
	public AccountListAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c);
		this.u = new Utils(context);
		this.df = DateUtils.getShortDateFormat(context);
	}		

	private static class AccountViewHolder {
		public TextView titleView;
		public TextView noteView;
		public TextView dateView;
		public TextView amountView;
		public ImageView iconView;
		public ImageView activeView;
	}
	
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);
		AccountViewHolder v = new AccountViewHolder();
		v.titleView = (TextView)view.findViewById(R.id.line1);
		v.noteView = (TextView)view.findViewById(R.id.note);
		v.dateView = (TextView)view.findViewById(R.id.label);
		v.amountView = (TextView)view.findViewById(R.id.date);
		v.iconView = (ImageView)view.findViewById(R.id.account_icon);
		v.activeView = (ImageView)view.findViewById(R.id.active_icon);
		view.setTag(v);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Account a = EntityManager.loadFromCursor(cursor, Account.class);
		AccountViewHolder v = (AccountViewHolder)view.getTag();

		v.titleView.setText(a.title);

		AccountType type = AccountType.valueOf(a.type);
		if (type.isCard && a.cardIssuer != null) {
			CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
			v.iconView.setImageResource(cardIssuer.iconId);
		} else {
			v.iconView.setImageResource(type.iconId);
		}
		if (a.isActive) {
			v.iconView.getDrawable().mutate().setAlpha(0xFF);
			v.activeView.setVisibility(View.INVISIBLE);			
		} else {
			v.iconView.getDrawable().mutate().setAlpha(0x77);
			v.activeView.setVisibility(View.VISIBLE);
		}

		StringBuilder sb = new StringBuilder();
		if (!Utils.isEmpty(a.issuer)) {
			sb.append(a.issuer);
		}
//		if (type.subTitleId > 0) {
//			sb.append(" / ").append(context.getString(type.subTitleId));
//		}
		if (!Utils.isEmpty(a.number)) {
			sb.append(" #"+a.number);
		}
		if (sb.length() == 0) {
			sb.append(context.getString(type.titleId));
		}
		v.noteView.setText(sb.toString());
		
		long date = a.creationDate;
		v.dateView.setText(df.format(new Date(date)));
		
		long amount = a.totalAmount;			
		u.setAmountText(v.amountView, a.currency, amount, false);
	}
	
	
	
}
