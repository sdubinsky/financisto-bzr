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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.R.id;
import ru.orangesoftware.financisto.R.layout;
import ru.orangesoftware.financisto.R.string;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.NodeInflater;
import ru.orangesoftware.financisto.view.NodeInflater.Builder;
import ru.orangesoftware.financisto.view.NodeInflater.CheckBoxBuilder;
import ru.orangesoftware.financisto.view.NodeInflater.EditBuilder;
import ru.orangesoftware.financisto.view.NodeInflater.ListBuilder;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ActivityLayout {

	private final Context context;
	private final ActivityLayoutListener listener;
	public final NodeInflater inflater;

	public ActivityLayout(Context context, ActivityLayoutListener listener) {
		this.context = context;
		this.inflater = new NodeInflater(context);
		this.listener = listener;
	}
	
	public View addTitleNode(LinearLayout layout, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
		return b.withLabel(labelId).create();
	}

	public View addTitleNodeNoDivider(LinearLayout layout, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_title);
		return b.withLabel(labelId).withNoDivider().create();
	}

	public void addListNodeSingle(LinearLayout layout, int id, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple_list);
		b.withId(id, listener).withLabel(labelId).create();
	}

	public void addInfoNodeSingle(LinearLayout layout, int id, int labelId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_single);
		b.withId(id, listener).withLabel(labelId).create();
	}

	public TextView addInfoNode(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
		return (TextView)v.findViewById(R.id.data);
	}
	
	public TextView addInfoNode(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_simple);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public View addListNodeIcon(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_icon);
		return b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();		
	}

	public View addListNodeIcon(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_icon);
		return b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
	}

	public View addListNode(LinearLayout layout, int id) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		return b.withId(id, listener).create();
	}
	
	public TextView addListNode(LinearLayout layout, int id, int labelId, int defaultValueResId) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNode(LinearLayout layout, int id, int labelId, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		View v = b.withId(id, listener).withLabel(labelId).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNode(LinearLayout layout, int id, String label, String defaultValue) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry);
		View v = b.withId(id, listener).withLabel(label).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public View addCheckboxNode(LinearLayout layout, int id) {
		Builder b = inflater.new Builder(layout, R.layout.select_entry_checkbox);
		return b.withId(id, listener).create();
	}
	
	public CheckBox addCheckboxNode(LinearLayout layout, int id, int labelId, int dataId, boolean checked) {
		CheckBoxBuilder b = inflater.new CheckBoxBuilder(layout);
		View v = b.withCheckbox(checked).withLabel(labelId).withId(id, listener).withData(dataId).create();
		return (CheckBox)v.findViewById(R.id.checkbox);
	}

	public void addInfoNodePlus(LinearLayout layout, int id, int plusId, int labelId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_simple_plus);
		b.withButtonId(plusId, listener).withLabel(labelId).withId(id, listener).create();
	}

	public TextView addListNodePlusWithoutDivider(LinearLayout layout, int id, int plusId, int labelId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus);
		View v = b.withButtonId(plusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValueResId).withNoDivider().create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNodePlusWithoutLabel(LinearLayout layout, int id, int plusId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus_no_label);
		View v = b.withButtonId(plusId, listener).withId(id, listener).withData(defaultValueResId).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNodePlus(LinearLayout layout, int id, int plusId, int labelId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_plus);
		View v = b.withButtonId(plusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNodeMinus(LinearLayout layout, int id, int minusId, int labelId, int defaultValueResId) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_minus);
		View v = b.withButtonId(minusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValueResId).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public TextView addListNodeMinus(LinearLayout layout, int id, int minusId, int labelId, String defaultValue) {
		ListBuilder b = inflater.new ListBuilder(layout, R.layout.select_entry_minus);
		View v = b.withButtonId(minusId, listener).withId(id, listener).withLabel(labelId).withData(defaultValue).create();
		return (TextView)v.findViewById(R.id.data);
	}

	public View addEditNode(LinearLayout layout, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.create();
	}

	public View addEditNode(LinearLayout layout, int labelId, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.withLabel(labelId).create();
	}

	public View addEditNode(LinearLayout layout, String label, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.withLabel(label).create();
	}

	public View addEditNodeWithoutDivider(LinearLayout layout, int labelId, View view) {
		EditBuilder b = inflater.new EditBuilder(layout, view);
		return b.withLabel(labelId).withNoDivider().create();
	}
	
	private void selectSingleChoice(int titleId, ListAdapter adapter, int checkedItem, 
			DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(context)
		.setSingleChoiceItems(adapter, checkedItem, onClickListener)
		.setTitle(titleId)
		.show();
	}
	
	public void selectMultiChoice(final int id, int titleId, final ArrayList<? extends MultiChoiceItem> items) {
		int count = items.size();
		String[] titles = new String[count];
		boolean[] checked = new boolean[count];
		for (int i=0; i<count; i++) {
			titles[i] = items.get(i).getTitle();
			checked[i] = items.get(i).isChecked();
		}
		new AlertDialog.Builder(context)
		.setMultiChoiceItems(titles, checked, new DialogInterface.OnMultiChoiceClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				items.get(which).setChecked(isChecked);
			}
		})
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.onSelected(id, items);
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		})
		.setTitle(titleId)
		.show();
	}
	
	public void select(final int id, int titleId, 
			final ListAdapter adapter, int selectedPosition) {		
		selectSingleChoice(titleId, adapter, selectedPosition, 
				new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();						
						listener.onSelectedPos(id, which);						
					}
		});
	}

	public void select(final int id, int titleId, 
			final Cursor cursor, final ListAdapter adapter, 
			final String idColumn, long valueId) {		
		int pos = Utils.moveCursor(cursor, idColumn, valueId);
		selectSingleChoice(titleId, adapter, pos, 
				new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();						
						cursor.moveToPosition(which);	
						long selectedId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumn)); 
						listener.onSelectedId(id, selectedId);						
					}
		});
	}

}
