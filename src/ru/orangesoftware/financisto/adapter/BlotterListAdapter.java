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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.recur.Recurrence;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.Date;
import java.util.HashMap;

import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;

public class BlotterListAdapter extends ResourceCursorAdapter {
	
	private final StringBuilder sb = new StringBuilder();
	private final Date dt = new Date();
	private final int transferColor;
	private final int futureColor;
	private final Drawable icBlotterIncome;
	private final Drawable icBlotterExpense;
	private final Drawable icBlotterTransfer;	
	private final Utils u;
	
	private final int colors[];
	
	private boolean allChecked = true;
	private final HashMap<Long, Boolean> checkedItems = new HashMap<Long, Boolean>();	
	
	public BlotterListAdapter(Context context, Cursor c) {
		this(context, R.layout.blotter_list_item, c, false);
	}

	public BlotterListAdapter(Context context, int layoutId, Cursor c) {
		this(context, layoutId, c, false);
	}

	public BlotterListAdapter(Context context, int layoutId, Cursor c, boolean autoRequery) {
		super(context, layoutId, c, autoRequery);
		transferColor = context.getResources().getColor(R.color.transfer_color);
		futureColor = context.getResources().getColor(R.color.future_color);
		icBlotterIncome = context.getResources().getDrawable(R.drawable.ic_blotter_income);
		icBlotterExpense = context.getResources().getDrawable(R.drawable.ic_blotter_expense);
		icBlotterTransfer = context.getResources().getDrawable(R.drawable.ic_blotter_transfer);
		u = new Utils(context);
		colors = initializeColors(context);
	}

	private int[] initializeColors(Context context) {
		Resources r = context.getResources();
		TransactionStatus[] statuses = TransactionStatus.values();
		int count = statuses.length;
		int[] colors = new int[count];
		for (int i=0; i<count; i++) {
			colors[i] = r.getColor(statuses[i].colorId);
		}
		return colors;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);		
		createHolder(view);
		return view;
	}

	protected void createHolder(View view) {
		BlotterViewHolder h = new BlotterViewHolder(view);
		view.setTag(h);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {	
		final BlotterViewHolder v = (BlotterViewHolder)view.getTag();
		long toAccountId = cursor.getLong(BlotterColumns.Indicies.TO_ACCOUNT_ID);
		int isTemplate = cursor.getInt(BlotterColumns.Indicies.IS_TEMPLATE);
		TextView noteView = isTemplate == 1 ? v.bottomView : v.centerView;
		if (toAccountId > 0) {
			v.topView.setText(R.string.transfer);			
			
			String fromAccountTitle = cursor.getString(BlotterColumns.Indicies.FROM_ACCOUNT_TITLE);
			String toAccountTitle = cursor.getString(BlotterColumns.Indicies.TO_ACCOUNT_TITLE);
			sb.setLength(0);
			sb.append(fromAccountTitle).append(" » ").append(toAccountTitle);
			noteView.setText(sb.toString());
			noteView.setTextColor(transferColor);

			long fromCurrencyId = cursor.getLong(BlotterColumns.Indicies.FROM_ACCOUNT_CURRENCY_ID);
			Currency fromCurrency = CurrencyCache.getCurrency(fromCurrencyId);
			long toCurrencyId = cursor.getLong(BlotterColumns.Indicies.TO_ACCOUNT_CURRENCY_ID);
			Currency toCurrency = CurrencyCache.getCurrency(toCurrencyId);
			
			int dateViewColor = v.bottomView.getCurrentTextColor();
			
			if (fromCurrencyId == toCurrencyId) {
				long amount = Math.abs(cursor.getLong(BlotterColumns.Indicies.FROM_AMOUNT));				
				u.setAmountText(v.rightView, fromCurrency, amount, false);					
				v.rightView.setTextColor(dateViewColor);
			} else {			
				long fromAmount = Math.abs(cursor.getLong(BlotterColumns.Indicies.FROM_AMOUNT));
				long toAmount = cursor.getLong(BlotterColumns.Indicies.TO_AMOUNT);
				sb.setLength(0);
				Utils.amountToString(sb, fromCurrency, fromAmount).append(" » ");
				Utils.amountToString(sb, toCurrency, toAmount);
				v.rightView.setText(sb.toString());	
				v.rightView.setTextColor(dateViewColor);
			}
			v.iconView.setImageDrawable(icBlotterTransfer);
		} else {
			String fromAccountTitle = cursor.getString(BlotterColumns.Indicies.FROM_ACCOUNT_TITLE);
			v.topView.setText(fromAccountTitle);
			sb.setLength(0);
            String payee = cursor.getString(BlotterColumns.Indicies.PAYEE);
			String note = cursor.getString(BlotterColumns.Indicies.NOTE);
			String location = cursor.getString(BlotterColumns.Indicies.LOCATION);
			long locationId = cursor.getLong(BlotterColumns.Indicies.LOCATION_ID);
			long categoryId = cursor.getLong(BlotterColumns.Indicies.CATEGORY_ID);
            String categoryTitle = "";
			if (categoryId > 0) {
                categoryTitle = cursor.getString(BlotterColumns.Indicies.CATEGORY_TITLE);
			}
            String text = generateTransactionText(sb, payee, note, locationId, location, categoryTitle);
            noteView.setText(text);
			noteView.setTextColor(Color.WHITE);
			
			long fromCurrencyId = cursor.getLong(BlotterColumns.Indicies.FROM_ACCOUNT_CURRENCY_ID);
			Currency fromCurrency = CurrencyCache.getCurrency(fromCurrencyId);
			long amount = cursor.getLong(BlotterColumns.Indicies.FROM_AMOUNT);
			sb.setLength(0);
			u.setAmountText(sb, v.rightView, fromCurrency, amount, true);
			if (amount > 0) {
				v.iconView.setImageDrawable(icBlotterIncome);
			} else if (amount < 0) {
				v.iconView.setImageDrawable(icBlotterExpense);
			}
		}
		if (isTemplate == 1) {
			String templateName = cursor.getString(BlotterColumns.Indicies.TEMPLATE_NAME);
			v.centerView.setText(templateName);
		} else {
			String recurrence = cursor.getString(BlotterColumns.Indicies.RECURRENCE);
			if (isTemplate == 2 && recurrence != null) {
				Recurrence r = Recurrence.parse(recurrence);
				//RRule rrule = r.createRRule();
				v.bottomView.setText(r.toInfoString(context));
				v.bottomView.setTextColor(v.topView.getTextColors().getDefaultColor());
			} else {
				TransactionStatus status = TransactionStatus.valueOf(cursor.getString(BlotterColumns.Indicies.STATUS));
				v.indicator.setBackgroundColor(colors[status.ordinal()]);
				long date = cursor.getLong(BlotterColumns.Indicies.DATETIME);
				dt.setTime(date);
				v.bottomView.setText(DateUtils.formatDateTime(context, dt.getTime(), 
						DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_ABBREV_MONTH));
				
				if (isTemplate == 0 && date > System.currentTimeMillis()) {
					v.bottomView.setTextColor(futureColor);
				} else {
					v.bottomView.setTextColor(v.topView.getTextColors().getDefaultColor());
				}
			}
		}
		if (v.checkBox != null) {
			final long id = cursor.getLong(BlotterColumns.Indicies.ID);
			v.checkBox.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					updateCheckedState(id, allChecked ^ v.checkBox.isChecked());
				}
			});
			boolean isChecked = getCheckedState(id);
			v.checkBox.setChecked(isChecked);
		}
	}

    public static String generateTransactionText(StringBuilder sb, String payee, String note,
                                           long locationId, String location, String categoryTitle) {
        sb.setLength(0);
        append(sb, payee);
        if (locationId > 0) {
            append(sb, location);
        }
        append(sb, note);
        String secondPart = sb.toString();
        sb.setLength(0);
        if (isNotEmpty(categoryTitle)) {
            if (isNotEmpty(secondPart)) {
                sb.append(categoryTitle).append(" (").append(secondPart).append(")");
                return sb.toString();
            } else {
                return categoryTitle;
            }
        } else {
            return secondPart;
        }
    }

    private static void append(StringBuilder sb, String s) {
        if (isNotEmpty(s)) {
            if (sb.length() > 0) {
                sb.append(": ");
            }
            sb.append(s);
        }
    }

    public boolean getCheckedState(long id) {
		return checkedItems.get(id) != null ? !allChecked : allChecked;
	}

	private void updateCheckedState(long id, boolean checked) {
		if (checked) {
			checkedItems.put(id, true);
		} else {
			checkedItems.remove(id);
		}
	}
	
	public int getCheckedCount() {
		return allChecked ? getCount()-checkedItems.size() : checkedItems.size();
	}

	public void checkAll() {
		allChecked = true;
		checkedItems.clear();
		notifyDataSetInvalidated();
	}

	public void uncheckAll() {
		allChecked = false;
		checkedItems.clear();
		notifyDataSetInvalidated();
	}

	public static class BlotterViewHolder {
		
		public final RelativeLayout layout;
		public final TextView indicator;
		public final TextView topView;
		public final TextView centerView;
		public final TextView bottomView;
		public final TextView rightView;
		public final ImageView iconView;
		public final CheckBox checkBox;
		
		public BlotterViewHolder(View view) {
			layout = (RelativeLayout)view.findViewById(R.id.layout);
			indicator = (TextView)view.findViewById(R.id.indicator);
			topView = (TextView)view.findViewById(R.id.top);
			centerView = (TextView)view.findViewById(R.id.center);		
			bottomView = (TextView)view.findViewById(R.id.bottom);
			rightView = (TextView)view.findViewById(R.id.right);
			iconView = (ImageView)view.findViewById(R.id.right_center);
			checkBox = (CheckBox)view.findViewById(R.id.cb);
		}
		
	}

	public long[] getAllCheckedIds() {
		int checkedCount = getCheckedCount();
		long[] ids = new long[checkedCount];
		int k = 0;
		if (allChecked) {
			int count = getCount();
			boolean addAll = count == checkedCount;
			for (int i=0; i<count; i++) {
				long id = getItemId(i);
				boolean checked = addAll || getCheckedState(id);
				if (checked) {
					ids[k++] = id;
				}
			}
		} else {
			for (Long id : checkedItems.keySet()) {
				ids[k++] = id;
			}
		}
		return ids;
	}

}
