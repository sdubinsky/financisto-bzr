/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.DateUtils.Period;

import java.text.DateFormat;
import java.util.Date;

public class ReportFilterActivity extends AbstractActivity {

	private WhereFilter filter = WhereFilter.empty();

	private TextView period;
	private DateFormat df;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.blotter_filter);

		df = DateUtils.getShortDateFormat(this);

		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);
		period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);

		Button bOk = (Button)findViewById(R.id.bOK);
		bOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent data = new Intent();
				filter.toIntent(data);
				setResult(RESULT_OK, data);
				finish();
			}
		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		ImageButton bNoFilter = (ImageButton)findViewById(R.id.bNoFilter);
		bNoFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
                setResult(RESULT_FIRST_USER);
                finish();
			}
		});		
		
		Intent intent = getIntent();
		if (intent != null) {
			filter = WhereFilter.fromIntent(intent);
            updatePeriodFromFilter();
		}
		
	}

    private void showMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
        v.setVisibility(View.VISIBLE);
    }

    private void hideMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
        v.setVisibility(View.GONE);
    }

    private ImageView findMinusButton(TextView textView) {
        LinearLayout layout = (LinearLayout) textView.getParent().getParent();
        return (ImageView) layout.getChildAt(layout.getChildCount()-1);
    }

	private void updatePeriodFromFilter() {
		DateTimeCriteria c = (DateTimeCriteria)filter.get(BlotterFilter.DATETIME);
		if (c != null) {
			Period p = c.getPeriod();
			if (p.isCustom()) {
				long periodFrom = c.getLongValue1();
				long periodTo = c.getLongValue2();
				period.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
			} else {
				period.setText(p.type.titleId);
			}
            showMinusButton(period);
		} else {
            clear(BlotterFilter.DATETIME, period);
		}
	}

	@Override
	protected void onClick(View v, int id) {
		switch (id) {
		case R.id.period:
			Intent intent = new Intent(this, DateFilterActivity.class);
			filter.toIntent(intent);
			startActivityForResult(intent, 1);
			break;
		case R.id.period_clear:
            clear(BlotterFilter.DATETIME, period);
			break;
		}
	}

	private void clear(String criteria, TextView textView) {
		filter.remove(criteria);
		textView.setText(R.string.no_filter);
        hideMinusButton(textView);
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
	}
	
	@Override
	public void onSelectedPos(int id, int selectedPos) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_FIRST_USER) {
				onClick(period, R.id.period_clear);
			} else if (resultCode == RESULT_OK) {
				DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
				filter.put(c);
				updatePeriodFromFilter();
			}
		}
	}
	
}
