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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.blotter.WhereFilter.DateTimeCriteria;
import ru.orangesoftware.financisto.utils.DateUtils;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.DateUtils.Period;
import ru.orangesoftware.financisto.utils.DateUtils.PeriodType;

import java.text.DateFormat;
import java.util.Date;

public abstract class AbstractImportActivity extends Activity {

    public static final int IMPORT_FILENAME_REQUESTCODE=0xff;
    public static final int IMPORT_XSLFILENAME_REQUESTCODE=0xfe;
    
    private final int layoutId;
	private DateFormat df;
	protected ImageButton bBrowse;
    protected EditText edFilename;


    public AbstractImportActivity(int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(layoutId);
		
		df = DateUtils.getShortDateFormat(this);
		
        bBrowse = (ImageButton) findViewById(R.id.btn_browse);
        bBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });
        edFilename=(EditText) findViewById(R.id.edFilename);

        internalOnCreate();
	}

    protected void openFile() {
    	String filename = edFilename.getText().toString();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(Uri.parse("file://" + filename));
        intent.setType("*/*");

        try {
            startActivityForResult(intent,IMPORT_FILENAME_REQUESTCODE);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
        }

	}

      
    protected abstract void internalOnCreate();

    protected abstract void updateResultIntentFromUi(Intent data);

    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode ==IMPORT_FILENAME_REQUESTCODE) {
			if (resultCode == RESULT_OK && data != null) {
				String filename = data.getDataString();
				if (filename != null) {
					if (filename.startsWith("file://")) {
						filename = filename.substring(7);
					}
					filename = Uri.decode(filename);
					edFilename.setText(filename);
					savePreferences();
				}
			}
		}

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
	}	
	
	abstract void savePreferences();
}
