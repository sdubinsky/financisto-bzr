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

import static ru.orangesoftware.financisto.utils.Utils.checkEditText;
import static ru.orangesoftware.financisto.utils.Utils.text;

import java.util.ArrayList;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.R.array;
import ru.orangesoftware.financisto.R.id;
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.adapter.CategoryListAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.db.DatabaseHelper.AttributeColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.CategoryColumns;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.utils.Utils;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class CategoryActivity extends AbstractActivity {
	
	public static final String CATEGORY_ID_EXTRA = "categoryId";
	public static final int NEW_ATTRIBUTE_REQUEST = 1;
	public static final int EDIT_ATTRIBUTE_REQUEST = 2;

	private String[] types;
	
	private Cursor attributeCursor;
	private ListAdapter attributeAdapter;
	
	private EditText categoryTitle;
	private TextView parentCategoryText;
	private Cursor categoryCursor;
	private CategoryListAdapter categoryAdapter;

	private ScrollView scrollView;
	private LinearLayout attributesLayout;
	private LinearLayout parentAttributesLayout;
	
	private Category category = new Category(-1);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.category);

		types = getResources().getStringArray(R.array.attribute_types);
		
		scrollView = (ScrollView)findViewById(R.id.scroll);
		
		categoryTitle = new EditText(this);
		categoryTitle.setSingleLine();
		
		Intent intent = getIntent();
		if (intent != null) {
			long id  = intent.getLongExtra(CATEGORY_ID_EXTRA, -1);
			if (id != -1) {
				category = db.getCategory(id);				
			}
		}

		attributeCursor = db.getAllAttributes();
		startManagingCursor(attributeCursor);
		attributeAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_dropdown_item, 
				attributeCursor, new String[]{AttributeColumns.NAME}, new int[]{android.R.id.text1});
		
		if (category.id == -1) {
			categoryCursor = db.getAllCategories(true);
		} else {
			categoryCursor = db.getAllCategoriesWithoutSubtree(category.id);
		}
		startManagingCursor(categoryCursor);
		//DatabaseUtils.dumpCursor(categoryCursor);
		
		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);		
		parentCategoryText = x.addListNode(layout, R.id.category, R.string.parent, R.string.select_category);
		x.addEditNode(layout, R.string.title, categoryTitle);							
		attributesLayout = (LinearLayout)x.addTitleNode(layout, R.string.attributes).findViewById(R.id.layout);
		x.addInfoNodePlus(attributesLayout, R.id.new_attribute, R.id.add_attribute, R.string.add_attribute);
		addAttributes();				
		parentAttributesLayout = (LinearLayout)x.addTitleNode(layout, R.string.parent_attributes).findViewById(R.id.layout);
		addParentAttributes();		
		
		categoryAdapter = new CategoryListAdapter(
				db, this, android.R.layout.simple_spinner_dropdown_item, categoryCursor);
		
		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				if (checkEditText(categoryTitle, "title", true, 100)) {						
					category.title = text(categoryTitle);
					int count = attributesLayout.getChildCount();
					ArrayList<Attribute> attributes = new ArrayList<Attribute>(count);
					for (int i=0; i<count; i++) {
						View v = attributesLayout.getChildAt(i);
						Object o = v.getTag();
						if (o instanceof Attribute) {
							attributes.add((Attribute)o);
						}
					}
					long id = db.insertOrUpdate(category, attributes);
					Intent data = new Intent();
					data.putExtra(DatabaseHelper.CategoryColumns.ID, id);
					setResult(RESULT_OK, data);
					finish();						
				}
			}		
		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				setResult(RESULT_CANCELED, null);
				finish();
			}		
		});	
		
		editCategory();
	}
	
	private void editCategory() {
		selectParentCategory(category.getParentId());
		categoryTitle.setText(category.title);
	}

	private void addAttributes() {
		long categoryId = category.id;
		if (categoryId == -1) {
			categoryId = 0;
		}				
		ArrayList<Attribute> attributes = db.getAttributesForCategory(categoryId);
		for (Attribute a : attributes) {
			addAttribute(a);
		}
	}

	private void addParentAttributes() {
		long categoryId = category.getParentId();
		ArrayList<Attribute> attributes = db.getAllAttributesForCategory(categoryId);
		if (attributes.size() > 0) {
			for (Attribute a : attributes) {
				View v = x.inflater.new Builder(parentAttributesLayout, R.layout.select_entry_simple).create();
				v.setTag(a);
				setAttributeData(v, a);
			}
		} else {
			x.addInfoNodeSingle(parentAttributesLayout, -1, R.string.no_attributes);
		}		
	}

	private void addAttribute(Attribute a) {		
		View v = x.inflater.new Builder(attributesLayout, R.layout.select_entry_simple_minus).withId(R.id.edit_attribute, this).create();
		setAttributeData(v, a);
		ImageView plusImageView = (ImageView)v.findViewById(R.id.plus_minus);
		plusImageView.setId(R.id.remove_attribute);
		plusImageView.setOnClickListener(this);
		plusImageView.setTag(v.getTag());
		v.setTag(a);
		scrollView.fullScroll(ScrollView.FOCUS_DOWN);
	}

	private void setAttributeData(View v, Attribute a) {
		TextView labelView = (TextView)v.findViewById(R.id.label);
		labelView.setText(a.name);		
		TextView dataView = (TextView)v.findViewById(R.id.data);
		dataView.setText(types[a.type-1]);		
	}

	@Override
	protected void onClick(View v, int id) {
		switch(id) {
			case R.id.category:				
				x.select(R.id.category, R.string.parent, categoryCursor, categoryAdapter, 
						CategoryColumns.ID, category.getParentId());
				break;
			case R.id.new_attribute:				
				x.select(R.id.new_attribute, R.string.attribute, attributeCursor, attributeAdapter, 
						AttributeColumns.ID, -1);
				break;
			case R.id.add_attribute: {
				Intent intent = new Intent(this, AttributeActivity.class);
				startActivityForResult(intent, NEW_ATTRIBUTE_REQUEST);				
			} break;
			case R.id.edit_attribute: {
				Object o = v.getTag();
				if (o instanceof Attribute) {
					Intent intent = new Intent(this, AttributeActivity.class);
					intent.putExtra(AttributeColumns.ID, ((Attribute)o).id);
					startActivityForResult(intent, EDIT_ATTRIBUTE_REQUEST);
				}
			} break;
			case R.id.remove_attribute:
				attributesLayout.removeView((View)v.getTag());
				attributesLayout.removeView((View)v.getParent());
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				break;
		}
	}	

	@Override
	public void onSelectedId(int id, long selectedId) {
		switch(id) {
			case R.id.category:
				selectParentCategory(selectedId);
				break;
			case R.id.new_attribute:
				Attribute a = db.getAttribute(selectedId);
				addAttribute(a);
				break;
		}
	}

	private void selectParentCategory(long parentId) {
		if (Utils.moveCursor(categoryCursor, CategoryColumns.ID, parentId) != -1) {
			String title = categoryCursor.getString(CategoryColumns.Indicies.TITLE);
			parentCategoryText.setText(title);						
			category.parent = new Category(parentId);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch(requestCode) {
			case NEW_ATTRIBUTE_REQUEST: {
				long attributeId = data.getLongExtra(AttributeColumns.ID, -1);
				if (attributeId != -1) {
					Attribute a = db.getAttribute(attributeId);
					addAttribute(a);
				}
			} break;
			case EDIT_ATTRIBUTE_REQUEST: {
				long attributeId = data.getLongExtra(AttributeColumns.ID, -1);
				if (attributeId != -1) {
					Attribute a = db.getAttribute(attributeId);
					attributeCursor.requery();
					updateAttribute(attributesLayout, a);
					updateAttribute(parentAttributesLayout, a);
				}
			} break;
			}
		}
	}

	private void updateAttribute(LinearLayout layout, Attribute a) {
		int count = layout.getChildCount();
		for (int i=0; i<count; i++) {
			View v = layout.getChildAt(i);
			Object o = v.getTag();
			if (o instanceof Attribute) {
				Attribute a2 = (Attribute)o;
				if (a2.id == a.id) {								
					setAttributeData(v, a);
				}
			}
		}
	}

}
