/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.utils.IntegrityCheck;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 8/21/12 10:28 PM
 */
public class IntegrityCheckTask extends AsyncTask<Void, Void, Boolean> {

    private final Activity activity;

    public IntegrityCheckTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Void... objects) {
        DatabaseAdapter db = new DatabaseAdapter(activity);
        IntegrityCheck check = new IntegrityCheck(db);
        return check.isBroken();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        activity.findViewById(R.id.integrity_error).setVisibility(result != null && result ? View.VISIBLE : View.GONE);
    }

}
