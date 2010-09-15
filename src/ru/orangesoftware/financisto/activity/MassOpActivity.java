package ru.orangesoftware.financisto.activity;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BlotterListAdapter;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;

public class MassOpActivity extends BlotterActivity {

	public MassOpActivity() {
		super(R.layout.blotter_mass_op);
	}

	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {
		bFilter = (ImageButton)findViewById(R.id.bFilter);
		bFilter.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MassOpActivity.this, BlotterFilterActivity.class);
				blotterFilter.toIntent(intent);
				startActivityForResult(intent, FILTER_REQUEST);
			}
		});
		blotterFilter = WhereFilter.empty();
	}
	
	@Override
	protected void applyFilter() {
		updateFilterImage();
	}
	
	@Override
	protected void calculateTotals() {
		// do nothing
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new BlotterListAdapter(this, R.layout.blotter_mass_op_list_item, cursor);
	}

}
