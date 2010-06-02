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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.CardIssuer;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CardIssuerAdapter extends BaseAdapter {
	
	private final static CardIssuer[] issuers = CardIssuer.values();
	private final LayoutInflater inflater;
	
	public CardIssuerAdapter(Context context) {
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return issuers.length;
	}

	@Override
	public Object getItem(int i) {
		return issuers[i];
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.account_type_list_item, parent, false);
		}
		ImageView icon = (ImageView)convertView.findViewById(R.id.icon);
		TextView title = (TextView)convertView.findViewById(R.id.line1);
		CardIssuer issuer = issuers[position];
		icon.setImageResource(issuer.iconId);
		title.setText(issuer.titleId);
		return convertView;
	}


}
