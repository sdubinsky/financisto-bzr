package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.utils.Utils.text;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public abstract class AbstractSplitActivity extends AbstractActivity {

    protected EditText noteText;
    protected TextView unsplitAmountText;

    protected Account fromAccount;
    protected Utils utils;
    protected Transaction split;

    private final int layoutId;

    protected AbstractSplitActivity(int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(layoutId);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_dialog_currency);

        fetchData();

        utils  = new Utils(this);
        split = Transaction.fromIntentAsSplit(getIntent());
        if (split.fromAccountId > 0) {
            fromAccount = db.em().getAccount(split.fromAccountId);
        }

        LinearLayout layout = (LinearLayout)findViewById(R.id.list);

        createUI(layout);
        createCommonUI(layout);
        updateUI();
    }

    private void createCommonUI(LinearLayout layout) {
        unsplitAmountText = x.addInfoNode(layout, R.id.add_split, R.string.unsplit_amount, "0");

        noteText = new EditText(this);
        x.addEditNode(layout, R.string.note, noteText);

        Button bSave = (Button) findViewById(R.id.bSave);
		bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                saveAndFinish();
            }
        });

        Button bCancel = (Button) findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    protected abstract void fetchData();

    protected abstract void createUI(LinearLayout layout);

    private void saveAndFinish() {
        Intent data = new Intent();
        updateFromUI();
        split.toIntentAsSplit(data);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    protected void updateFromUI() {
        split.note = text(noteText);
    }

    protected abstract void updateUI();

    protected void setNote(String note) {
        noteText.setText(note);
    }

    protected void setUnsplitAmount(long amount) {
        Currency currency = fromAccount != null ? fromAccount.currency : Currency.defaultCurrency();
        utils.setAmountText(unsplitAmountText, currency, amount, false);
    }

}
