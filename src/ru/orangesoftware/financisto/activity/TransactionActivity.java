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

import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.AmountInput.OnAmountChangedListener;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;
import static ru.orangesoftware.financisto.utils.Utils.text;

public class TransactionActivity extends AbstractTransactionActivity {

	public static final String CURRENT_BALANCE_EXTRA = "accountCurrentBalance";

	private static final int MENU_TURN_GPS_ON = Menu.FIRST;
    private static final int SPLIT_REQUEST = 5001;

    private final AtomicLong idSequence = new AtomicLong();
    private final IdentityHashMap<View, Split> viewToSplitMap = new IdentityHashMap<View, Split>();

    private AutoCompleteTextView payeeText;
    private SimpleCursorAdapter payeeAdapter;
	private TextView differenceText;
	private boolean isUpdateBalanceMode = false;
    private boolean isShowPayee = true;
	private long currentBalance;
	private Utils u;

    private LinearLayout splitsLayout;
    private TextView unsplitAmountText;

	public TransactionActivity() {
	}

	protected int getLayoutId() {
		return MyPreferences.isUseFixedLayout(this) ? R.layout.transaction_fixed : R.layout.transaction_free;
	}

	@Override
	protected void internalOnCreate() {
		u = new Utils(this);
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(CURRENT_BALANCE_EXTRA)) {
				currentBalance = intent.getLongExtra(CURRENT_BALANCE_EXTRA, 0);
				isUpdateBalanceMode = true;
			}
		}
		if (transaction.isTemplateLike()) {
			setTitle(transaction.isTemplate() ? R.string.transaction_template : R.string.transaction_schedule);
			if (transaction.isTemplate()) {
				dateText.setEnabled(false);
				timeText.setEnabled(false);
			}
		}
	}

    @Override
    protected Cursor fetchCategories() {
        if (isUpdateBalanceMode) {
            return db.getCategories(true);
        } else {
            return db.getAllCategories();
        }
    }

    @Override
	protected void createListNodes(LinearLayout layout) {
		//account
		accountText = x.addListNode(layout, R.id.account, R.string.account, R.string.select_account);
        //payee
        isShowPayee = MyPreferences.isShowPayee(this);
        if (isShowPayee) {
            payeeAdapter = TransactionUtils.createPayeeAdapter(this, db);
            payeeText = new AutoCompleteTextView(this);
            payeeText.setThreshold(1);
            payeeText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        payeeText.setAdapter(payeeAdapter);
                        payeeText.selectAll();
                    }
                }
            });
            x.addEditNode(layout, R.string.payee, payeeText);
        }
		//category
		categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
		//amount
		amountInput = new AmountInput(this);
		amountInput.setOwner(this);
		x.addEditNode(layout, isUpdateBalanceMode ? R.string.new_balance : R.string.amount, amountInput);
		// difference
		if (isUpdateBalanceMode) {
			differenceText = x.addInfoNode(layout, -1, R.string.difference, "0");
			amountInput.setAmount(currentBalance);
			amountInput.setOnAmountChangedListener(new OnAmountChangedListener(){
				@Override
				public void onAmountChanged(long oldAmount, long newAmount) {
					long balanceDifference = newAmount-currentBalance;
					u.setAmountText(differenceText, amountInput.getCurrency(), balanceDifference, true);
				}
			});
            if (currentBalance >= 0) {
                amountInput.setIncome();
            } else {
                amountInput.setExpense();
            }
		} else {
            createSplitsLayout(layout);
            amountInput.setOnAmountChangedListener(new OnAmountChangedListener() {
                @Override
                public void onAmountChanged(long oldAmount, long newAmount) {
                    updateUnsplitAmount();
                }
            });
        }
	}

    private void createSplitsLayout(LinearLayout layout) {
        splitsLayout = new LinearLayout(this);
        splitsLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(splitsLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void addOrRemoveSplits() {
        if (splitsLayout == null) {
            return;
        }
        if (selectedCategoryId == Category.SPLIT_CATEGORY_ID) {
            View v = x.addNodePlus(splitsLayout, R.id.add_split, R.id.add_split, R.string.unsplit_amount, "0");
            unsplitAmountText = (TextView)v.findViewById(R.id.data);
            updateUnsplitAmount();
        } else {
            splitsLayout.removeAllViews();
        }
    }

    private void updateUnsplitAmount() {
        if (unsplitAmountText != null) {
            long amountDifference = calculateUnsplitAmount();
            u.setAmountText(unsplitAmountText, amountInput.getCurrency(), amountDifference, false);
        }
    }

    private long calculateUnsplitAmount() {
        long splitAmount = calculateSplitAmount();
        return amountInput.getAmount()-splitAmount;
    }

    private long calculateSplitAmount() {
        long amount = 0;
        for (Split split : viewToSplitMap.values()) {
            amount += split.amount;
        }
        return amount;
    }

    protected void switchIncomeExpenseButton(Category category) {
        if (!isUpdateBalanceMode) {
            if (category.isIncome()) {
                amountInput.setIncome();
            } else {
                amountInput.setExpense();
            }
        }
    }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
			accountText.requestFocusFromTouch();
		}
	}

	@Override
	protected boolean onOKClicked() {
		if (checkSelectedId(selectedAccountId, R.string.select_account) &&
            checkUnsplitAmount()) {
			updateTransactionFromUI();
			return true;
		}
		return false;
	}

    private boolean checkUnsplitAmount() {
        if (selectedCategoryId == Category.SPLIT_CATEGORY_ID) {
            long unsplitAmount = calculateUnsplitAmount();
            if (unsplitAmount != 0) {
                Toast.makeText(this, R.string.unsplit_amount_greater_than_zero, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    @Override
	protected void editTransaction(Transaction transaction) {
        selectAccount(transaction.fromAccountId, false);
        commonEditTransaction(transaction);
        fetchSplits();
        selectPayee(transaction.payeeId);
		amountInput.setAmount(transaction.fromAmount);
	}

    private void fetchSplits() {
        List<Split> splits = em.getSplitsForTransaction(transaction.id);
        for (Split split : splits) {
            addOrEditSplit(split);
        }
    }

    private void updateTransactionFromUI() {
		updateTransactionFromUI(transaction);
        if (isShowPayee) {
            transaction.payeeId = db.insertPayee(text(payeeText));
        }
		transaction.fromAccountId = selectedAccountId;
		long amount = amountInput.getAmount();
		if (isUpdateBalanceMode) {
			amount -= currentBalance;
		}
		transaction.fromAmount = amount;
        transaction.splits = new LinkedList<Split>(viewToSplitMap.values());
	}

    private void selectPayee(long payeeId) {
        if (isShowPayee) {
            Payee p = db.em().get(Payee.class, payeeId);
            selectPayee(p);
        }
    }

    protected void selectPayee(Payee p) {
        if (isShowPayee && p != null) {
            payeeText.setText(p.title);
            transaction.payeeId = p.id;
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menuItem = menu.add(0, MENU_TURN_GPS_ON, 0, R.string.force_gps_location);
		menuItem.setIcon(android.R.drawable.ic_menu_mylocation);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MENU_TURN_GPS_ON:
			selectCurrentLocation(true);
			break;
		}
		return false;
	}

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        switch (id) {
            case R.id.add_split:
                createSplit();
                break;
            case R.id.delete_split:
                View parentView = (View)v.getParent();
                deleteSplit(parentView);
                break;
        }
        Split split = viewToSplitMap.get(v);
        if (split != null) {
            split.unsplitAmount = split.amount + calculateUnsplitAmount();
            editSplit(split);
        }
    }

    private void createSplit() {
        Split split = new Split();
        split.id = idSequence.decrementAndGet();
        split.accountId = selectedAccountId;
        split.amount = split.unsplitAmount = calculateUnsplitAmount();
        editSplit(split);
    }

    private void editSplit(Split split) {
        Intent intent = new Intent(this, SplitActivity.class);
        split.toIntent(intent);
        startActivityForResult(intent, SPLIT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPLIT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Split split = Split.fromIntent(data);
                addOrEditSplit(split);
            }
        }
    }

    private void addOrEditSplit(Split split) {
        View v = findView(split);
        if (v  == null) {
            v = x.addNodeMinus(splitsLayout, R.id.edit_aplit, R.id.delete_split, R.string.split, "");
        }
        TextView label = (TextView)v.findViewById(R.id.label);
        TextView data = (TextView)v.findViewById(R.id.data);
        label.setText(createSplitText(split));
        Currency currency = amountInput.getCurrency();
        u.setAmountText(data, currency, split.amount, false);
        viewToSplitMap.put(v, split);
        updateUnsplitAmount();
    }

    private String createSplitText(Split split) {
        StringBuilder sb = new StringBuilder();
        Category category = db.getCategory(split.categoryId);
        sb.append(category.title);
        if (isNotEmpty(split.note)) {
            sb.append(" (").append(split.note).append(")");
        }
        return sb.toString();
    }

    private View findView(Split split) {
        for (Map.Entry<View, Split> entry : viewToSplitMap.entrySet()) {
            Split s = entry.getValue();
            if (s.id == split.id) {
                return  entry.getKey();
            }
        }
        return null;
    }

    private void deleteSplit(View v) {
        Split split = viewToSplitMap.remove(v);
        if (split != null) {
            removeSplitView(v);
            updateUnsplitAmount();
        }
    }

    private void removeSplitView(View v) {
        splitsLayout.removeView(v);
        View dividerView = (View)v.getTag();
        if (dividerView != null) {
            splitsLayout.removeView(dividerView);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("Financisto", "TransactionActivity.onDestroy");
        if (payeeAdapter != null) {
            payeeAdapter.changeCursor(null);
        }
        super.onDestroy();
    }
}
