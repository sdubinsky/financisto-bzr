package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.widget.AmountInput;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/21/11 7:17 PM
 */
public class SplitTransactionActivity extends AbstractSplitActivity {

    protected AmountInput amountInput;

    protected TextView categoryText;
    protected Cursor categoryCursor;
    protected ListAdapter categoryAdapter;

    public SplitTransactionActivity() {
        super(R.layout.split_fixed);
    }

    @Override
    protected void createUI(LinearLayout layout) {
        categoryText = x.addListNode(layout, R.id.category, R.string.category, R.string.select_category);

        amountInput = new AmountInput(this);
        amountInput.setOwner(this);
        amountInput.setOnAmountChangedListener(new AmountInput.OnAmountChangedListener() {
            @Override
            public void onAmountChanged(long oldAmount, long newAmount) {
                setUnsplitAmount(split.unsplitAmount - newAmount);
            }
        });
        x.addEditNode(layout, R.string.amount, amountInput);
    }

    @Override
    protected void fetchData() {
        categoryCursor = db.getCategories(true);
        startManagingCursor(categoryCursor);
        categoryAdapter = TransactionUtils.createCategoryAdapter(db, this, categoryCursor);
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        selectCategory(split.categoryId);
        setAmount(split.fromAmount);
    }

    @Override
    protected void updateFromUI() {
        super.updateFromUI();
        split.fromAmount = amountInput.getAmount();
    }

    private void selectCategory(long categoryId) {
        Category category = em.getCategory(categoryId);
        if (category != null) {
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

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        if (id == R.id.category) {
            if (!CategorySelectorActivity.pickCategory(this, split.categoryId, false)) {
                x.select(this, R.id.category, R.string.category, categoryCursor, categoryAdapter,
                        DatabaseHelper.CategoryViewColumns._id.name(), split.categoryId);
            }
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
            if (requestCode == CategorySelectorActivity.PICK_CATEGORY_REQUEST) {
                long categoryId = data.getLongExtra(CategorySelectorActivity.SELECTED_CATEGORY_ID, 0);
                selectCategory(categoryId);
            }
        }
    }

}
