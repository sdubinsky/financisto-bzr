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

import java.util.ArrayList;

import ru.orangesoftware.financisto.graph.Amount;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.graph.GraphWidget;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ReportAdapter extends BaseAdapter {
		
	private final Context context; 
	private final ArrayList<GraphUnit> units;

	private long maxAmount = 0;
	private long maxAmountWidth = 0;
	
	public ReportAdapter(Context context, ArrayList<GraphUnit> units) {
		this.context = context;
		this.units = units;
		Rect rect = new Rect();
		for (GraphUnit u : units) {
			for (Amount a : u.amounts.values()) {
				String amountText = a.getAmountText();
				u.style.amountPaint.getTextBounds(amountText, 0, amountText.length(), rect);
				a.amountTextWidth = (int)rect.width();
				a.amountTextHeight = (int)rect.height();
				maxAmount = Math.max(maxAmount, Math.abs(a.amount));
				maxAmountWidth = Math.max(maxAmountWidth, a.amountTextWidth);			
			}			
		}
	}

	@Override
	public int getCount() {
		return units.size();
	}

	@Override
	public Object getItem(int position) {
		return units.get(position);
	}

	@Override
	public long getItemId(int position) {
		return units.get(position).id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		GraphUnit unit = units.get(position);
		GraphWidget w = new GraphWidget(context, unit, maxAmount, maxAmountWidth);
		w.setPadding(5, 10, 5, 5);
		return w;
	}

}
