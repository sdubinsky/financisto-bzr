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
import java.util.HashMap;
import java.util.HashSet;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Category;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class CategoryListAdapter2 extends BaseAdapter {

	private static final int P = 10;
	
	private final Context context;
	private ArrayList<Category> categories;

	private final ArrayList<Category> list = new ArrayList<Category>();
	private final HashSet<Long> state = new HashSet<Long>();
	
	private final Drawable expandedDrawable;
	private final Drawable collapsedDrawable;
	
	public CategoryListAdapter2(Context context, ArrayList<Category> categories) {
		this.context = context;
		this.categories = categories;
		this.expandedDrawable = context.getResources().getDrawable(R.drawable.expander_ic_maximized);
		this.collapsedDrawable = context.getResources().getDrawable(R.drawable.expander_ic_minimized);
		recreatePlainList();
	}
	
	private void recreatePlainList() {
		list.clear();
		addCategories(categories);
	}

	private void addCategories(ArrayList<Category> categories) {
		if (categories == null || categories.isEmpty()) {
			return;
		}
		for (Category c : categories) {
			list.add(c);
			if (state.contains(c.id)) {
				addCategories(c.children);
			}
		}
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Category getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final Category c = getItem(position);
		TextView tv = new TextView(context);
		tv.setHeight(60);
		tv.setGravity(Gravity.CENTER_VERTICAL);
		tv.setText(String.valueOf(c));
		if (c.hasChildren()) {
			LinearLayout layout = new LinearLayout(context);
			ImageView image = new ImageView(context);
			image.setImageDrawable(state.contains(c.id) ? expandedDrawable : collapsedDrawable);
			image.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					onListItemClick(position, c.id);
				}
			});
			LinearLayout.LayoutParams p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			p.gravity = Gravity.CENTER_VERTICAL;
			layout.addView(image, p);
			tv.setPadding(3, 0, 0, 0);
			layout.addView(tv);
			layout.setPadding(P*(c.level-1), 0, 0, 0);
			return layout;
		} else {
			tv.setPadding(P*c.level, 0, 0, 0);
			return tv;			
		}
	}

	public void onListItemClick(int position, long id) {
		if (state.contains(id)) {
			state.remove(id);
		} else {
			state.add(id);
		}
		recreatePlainList();
		notifyDataSetChanged();
	}

	public void setCategories(ArrayList<Category> categories) {
		this.categories = categories;
		recreatePlainList();
	}

	public void setAttributes(HashMap<Long, String> attributes) {
		// TODO Auto-generated method stub		
	}

}
