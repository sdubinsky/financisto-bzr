package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Split;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;

import static ru.orangesoftware.financisto.utils.Utils.text;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public class SplitActivity extends AbstractActivity {

    protected AmountInput amountInput;

    protected TextView categoryText;
    protected Cursor categoryCursor;
    protected ListAdapter categoryAdapter;

    protected EditText noteText;
    protected TextView unsplitAmountText;

    private Account account;
    private Utils utils;
    private Split split;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.currency);
        setContentView(R.layout.split_fixed);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_dialog_currency);

        fetchData();
        createUI();
        updateUI();
    }

    private void fetchData() {
        categoryCursor = db.getCategories(true);
        startManagingCursor(categoryCursor);
        categoryAdapter = TransactionUtils.createCategoryAdapter(db, this, categoryCursor);

        utils  = new Utils(this);

        split = Split.fromIntent(getIntent());
        if (split.accountId > 0) {
            account = db.em().getAccount(split.accountId);
        }
    }

    private void createUI() {
        LinearLayout layout = (LinearLayout)findViewById(R.id.list);

        categoryText = x.addListNode(layout, R.id.category, R.string.category, R.string.select_category);

        amountInput = new AmountInput(this);
        amountInput.setOwner(this);
        amountInput.setOnAmountChangedListener(new AmountInput.OnAmountChangedListener() {
            @Override
            public void onAmountChanged(long oldAmount, long newAmount) {
                split.amount = newAmount;
                setUnsplitAmount(split.unsplitAmount - newAmount);
            }
        });
        x.addEditNode(layout, R.string.amount, amountInput);

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

    private void saveAndFinish() {
        Intent data = new Intent();
        split.note = text(noteText);
        split.toIntent(data);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void updateUI() {
        selectCategory(split.categoryId);
        setAmount(split.amount);
        setNote(split.note);
    }

    private void selectCategory(long categoryId) {
        if (Utils.moveCursor(categoryCursor, DatabaseHelper.CategoryViewColumns._id.name(), categoryId) != -1) {
            Category category = Category.formCursor(categoryCursor);
            categoryText.setText(Category.getTitle(category.title, category.level));
            if (category.isIncome()) {
                amountInput.setIncome();
            } else {
                amountInput.setExpense();
            }
            split.categoryId = categoryId;
        }
    }

    private void setAmount(long amount) {
        amountInput.setAmount(amount);
    }

    private void setNote(String note) {
        noteText.setText(note);
    }

    private void setUnsplitAmount(long amount) {
        Currency currency = account != null ? account.currency : Currency.defaultCurrency();
        utils.setAmountText(unsplitAmountText, currency, amount, false);
    }

    @Override
    protected void onClick(View v, int id) {
        if (id == R.id.category) {
            x.select(this, R.id.category, R.string.category, categoryCursor, categoryAdapter,
                    DatabaseHelper.CategoryViewColumns._id.name(), split.categoryId);
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        switch(id) {
            case R.id.category:
                selectCategory(selectedId);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            amountInput.processActivityResult(requestCode, data);
        }
    }

}
