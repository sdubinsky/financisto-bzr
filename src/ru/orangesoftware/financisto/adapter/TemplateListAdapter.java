package ru.orangesoftware.financisto.adapter;

import ru.orangesoftware.financisto.R;
import android.content.Context;
import android.database.Cursor;
import android.view.View;

public class TemplateListAdapter extends BlotterListAdapter {

	private int multiplier = 1;

	public TemplateListAdapter(Context context, Cursor c) {
		super(context, R.layout.template_list_item_2, c);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);
		if (multiplier > 1) {
			BlotterViewHolder v = (BlotterViewHolder)view.getTag();
			v.rightView.append(" x"+multiplier);
		}
	}

	public void incrementMultiplier() {
		++multiplier;
		notifyDataSetInvalidated();
	}

	public void decrementMultiplier() {
		if (multiplier > 1) {
			--multiplier;
			notifyDataSetInvalidated();
		}
	}
	
	public int getMultiplier() {
		return multiplier;
	}

}
