/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - adding 2D chart reports
 ******************************************************************************/
package ru.orangesoftware.financisto.report;

import ru.orangesoftware.financisto.R;
import android.content.Context;
import android.os.Bundle;

public enum ReportType {

	BY_PERIOD(R.string.report_by_period, R.string.report_by_period_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return new PeriodReport(context);
		}
	},
	BY_CATEGORY(R.string.report_by_category, R.string.report_by_category_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return new CategoryReport(context);
		}
	},
	BY_SUB_CATEGORY_ROOTS(R.string.report_by_sub_category, R.string.report_by_sub_category_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return new CategoryReport2(context, extra);
		}
	},
	BY_SUB_CATEGORY(R.string.report_by_category, R.string.report_by_category_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return new SubCategoryReport(context, extra);
		}
	},
	BY_LOCATION(R.string.report_by_location, R.string.report_by_location_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return new LocationsReport(context);
		}
	},
	BY_PROJECT(R.string.report_by_project, R.string.report_by_project_summary, R.drawable.report_icon_default){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return new ProjectsReport(context);
		}
	}, 
	BY_ACCOUNT_BY_PERIOD(R.string.report_by_account_by_period, R.string.report_by_account_by_period_summary, R.drawable.ic_tab_2d_graph_selected){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	}, 
	BY_CATEGORY_BY_PERIOD(R.string.report_by_category_by_period, R.string.report_by_category_by_period_summary, R.drawable.ic_tab_2d_graph_selected){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	}, 
	BY_LOCATION_BY_PERIOD(R.string.report_by_location_by_period, R.string.report_by_location_by_period_summary, R.drawable.ic_tab_2d_graph_selected){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	}, 
	BY_PROJECT_BY_PERIOD(R.string.report_by_project_by_period, R.string.report_by_project_by_period_summary, R.drawable.ic_tab_2d_graph_selected){
		@Override
		public Report createReport(Context context, Bundle extra) {
			return null;
		}
		
		@Override
		public boolean isConventionalBarReport() {
			return false;
		}
	};
	
	public final int titleId;
	public final int summaryId;
	public final int iconId;
	
	ReportType(int titleId, int summaryId, int iconId) {
		this.titleId = titleId;
		this.summaryId = summaryId;
		this.iconId = iconId;
	}
	
	public boolean isConventionalBarReport() {
		return true;
	}
	
	public abstract Report createReport(Context context, Bundle extra);

}
