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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Project;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ProjectListAdapter extends BaseAdapter {
	
	private final LayoutInflater inflater;
	
	private ArrayList<Project> projects;
	
	public ProjectListAdapter(Context context, ArrayList<Project> projects) {
		this.projects = projects;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public void setProjects(ArrayList<Project> projects) {
		this.projects = projects;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return projects.size();
	}

	@Override
	public Project getItem(int i) {
		return projects.get(i);
	}

	@Override
	public long getItemId(int i) {
		return getItem(i).id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		GenericViewHolder v;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.generic_list_item, parent, false);
			v = GenericViewHolder.createAndTag(convertView);
		} else {
			v = (GenericViewHolder)convertView.getTag();
		}
		v.labelView.setVisibility(View.GONE);
		v.amountView.setVisibility(View.GONE);
		
		Project p = getItem(position);
		v.lineView.setText(p.title);
		return convertView;
	}

}
