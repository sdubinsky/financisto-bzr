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

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.view.PinView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class PinActivity extends Activity implements PinView.PinListener {
	
	public static final String SUCCESS = "PIN_SUCCESS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		String pin = MyPreferences.getPin(this);
		if (pin == null) {
			Toast.makeText(this, R.string.pin_protection_notify, Toast.LENGTH_SHORT).show();
			setResult(RESULT_OK);
			finish();
		}
		PinView v = new PinView(this, this, pin);
		setContentView(v.getView());
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		((TextView)findViewById(android.R.id.title)).setText(R.string.pin);
	}

	@Override
	public void onConfirm(String pinBase64) {		
	}

	@Override
	public void onSuccess(String pinBase64) {
		MyPreferences.setPinRequired(false);
		Intent data = new Intent();
		data.putExtra(SUCCESS, true);
		setResult(RESULT_OK, data);
		finish();
	}

}
