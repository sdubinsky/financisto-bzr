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
package ru.orangesoftware.financisto.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.R.drawable;
import ru.orangesoftware.financisto.R.id;
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.adapter.CategoryListAdapter2;
import ru.orangesoftware.financisto.model.Category;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class CategoryListActivity2 extends AbstractListActivity {
	
	private static final int NEW_CATEGORY_REQUEST = 1;
	private static final int EDIT_CATEGORY_REQUEST = 2;

	public CategoryListActivity2() {
		super(R.layout.category_list);
	}

	private ArrayList<Category> categories;
	private HashMap<Long, String> attributes;
	
	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		super.internalOnCreate(savedInstanceState);
		categories = db.getAllCategoriesTree(false);
		attributes = db.getAllAttributesMap();
		ImageButton bAttributes = (ImageButton)findViewById(R.id.bAttributes);
		bAttributes.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(CategoryListActivity2.this, AttributeListActivity.class);
				startActivityForResult(intent, 0);
			}
		});
	}

	@Override
	protected void addItem() {
		Intent intent = new Intent(CategoryListActivity2.this, CategoryActivity.class);
		startActivityForResult(intent, NEW_CATEGORY_REQUEST);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new CategoryListAdapter2(this, categories);
	}

	@Override
	protected Cursor createCursor() {
		return null;
	}
	
	@Override
	public void requeryCursor() {
		long t0 = System.currentTimeMillis();
		categories = db.getAllCategoriesTree(false);
		attributes = db.getAllAttributesMap();
		updateAdapter();
		long t1 = System.currentTimeMillis();
		Log.d("CategoryListActivity", "Requery in "+(t1-t0)+"ms");
	}

	private void updateAdapter() {
		((CategoryListAdapter2)adapter).setCategories(categories);
		((CategoryListAdapter2)adapter).setAttributes(attributes);
		((CategoryListAdapter2)adapter).notifyDataSetChanged();
	}

	@Override
	protected void deleteItem(int position, final long id) {
		Category c = (Category)getListAdapter().getItem(position);
		new AlertDialog.Builder(this)
			.setTitle(c.getTitle())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(R.string.delete_category_dialog)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					db.deleteCategory(id);
					requeryCursor();
				}				
			})
			.setNegativeButton(R.string.no, null)
			.show();		
	}

	@Override
	public void editItem(int position, long id) {
		Intent intent = new Intent(CategoryListActivity2.this, CategoryActivity.class);
		intent.putExtra(CategoryActivity.CATEGORY_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_CATEGORY_REQUEST);		
	}

	@Override
	protected void viewItem(final int position, long id) {
		final Category c = (Category)getListAdapter().getItem(position);
		final ArrayList<PositionAction> actions = new ArrayList<PositionAction>();
		Category p = c.parent;
		ArrayList<Category> categories;  
		if (p == null) {
			categories = this.categories;
		} else {
			categories = p.children;
		}
		int pos = categories.indexOf(c);
		if (pos > 0) {
			actions.add(top);
			actions.add(up);
		}
		if (pos < categories.size() - 1) {
			actions.add(down);
			actions.add(bottom);
		}
		actions.add(addSibling);
		actions.add(addChild);
		actions.add(delete);
		final ListAdapter a = new CategoryPositionListAdapter(actions);
		new AlertDialog.Builder(this)
			.setTitle(c.getTitle())
			.setAdapter(a, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					PositionAction action = actions.get(which);
					switch (action.id) {
					case 2:
						// up
						moveCategory(c, -1);
						break;
					case 3:
						// down
						moveCategory(c, +1);
						break;
					}
				}
			})
			.show();		
	}	
	
	protected void moveCategory(Category c, int k) {
		boolean recreateCursor = false;
		if (c.parent == null) {
			recreateCursor = findAndExchangeSortOrders(this.categories, c, k);
		} else {
			recreateCursor = findAndExchangeSortOrders(c.parent.children, c, k);
		}
		if (recreateCursor) {
			sortCategories(categories);
			updateAdapter();
		}
	}
	
	private boolean findAndExchangeSortOrders(ArrayList<Category> categories, Category c, int k) {
		int count = categories.size();
		for (int i=0; i<count; i++) {				
			if (categories.get(i) == c) {	
				int j = i+k;
				if (j >= 0 && j <count) {
					Category c2 = categories.get(i+k);
					int sortOrder = c.sortOrder;
					c.sortOrder = c2.sortOrder;
					c2.sortOrder = sortOrder;
					return true;
				}
			}
		}
		return false;
	}

	private void sortCategories(ArrayList<Category> categories) {
		Collections.sort(categories, byOrderComaprator);
		for (Category c : categories) {
			if (c.hasChildren()) {
				sortCategories(c.children);
			}
		}
	}

	private static final Comparator<Category> byOrderComaprator = new Comparator<Category>() {

		@Override
		public int compare(Category c1, Category c2) {
			if (c1.sortOrder == c2.sortOrder) {
				return c1.title.compareTo(c2.title);
			} else {
				return c1.sortOrder - c2.sortOrder;
			}
		}
		
	};

	private static class PositionAction {
		final int id;
		final int icon;
		final int title;
		public PositionAction(int id, int icon, int title) {
			this.id = id;
			this.icon = icon;
			this.title = title;
		}
	}
	
	private static final PositionAction top = new PositionAction(1, R.drawable.ic_btn_round_top, R.string.position_move_top);
	private static final PositionAction up = new PositionAction(2, R.drawable.ic_btn_round_up, R.string.position_move_up);
	private static final PositionAction down = new PositionAction(3, R.drawable.ic_btn_round_down, R.string.position_move_down);
	private static final PositionAction bottom = new PositionAction(4, R.drawable.ic_btn_round_bottom, R.string.position_move_bottom);
	private static final PositionAction addSibling = new PositionAction(5, R.drawable.ic_btn_round_plus, R.string.add_sibling);
	private static final PositionAction addChild = new PositionAction(6, R.drawable.ic_btn_round_plus, R.string.add_child);
	private static final PositionAction delete = new PositionAction(7, R.drawable.ic_btn_round_minus, R.string.delete);

	private class CategoryPositionListAdapter extends BaseAdapter {
		
		private final ArrayList<PositionAction> actions;
		
		public CategoryPositionListAdapter(ArrayList<PositionAction> actions) {
			this.actions = actions;
		}

		@Override
		public int getCount() {
			return actions.size();
		}

		@Override
		public PositionAction getItem(int position) {
			return actions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return actions.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.position_list_item, parent, false);
			}
			ImageView v = (ImageView)convertView.findViewById(R.id.icon);
			TextView t = (TextView)convertView.findViewById(R.id.line1);
			PositionAction a = actions.get(position);
			v.setImageResource(a.icon);
			t.setText(a.title);			
			return convertView;
		}
		
	}
	
	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.category);
	}

}
