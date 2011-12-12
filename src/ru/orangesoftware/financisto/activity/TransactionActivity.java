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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.SplitAdjuster;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.AmountInput.OnAmountChangedListener;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static ru.orangesoftware.financisto.model.Category.isSplit;
import static ru.orangesoftware.financisto.utils.AndroidUtils.isSupportedApiLevel;
import static ru.orangesoftware.financisto.utils.Utils.isNotEmpty;
import static ru.orangesoftware.financisto.utils.Utils.text;

public class TransactionActivity extends AbstractTransactionActivity {

	public static final String CURRENT_BALANCE_EXTRA = "accountCurrentBalance";

	private static final int MENU_TURN_GPS_ON = Menu.FIRST;
    private static final int SPLIT_REQUEST = 5001;

    private final AtomicLong idSequence = new AtomicLong();
    private final IdentityHashMap<View, Transaction> viewToSplitMap = new IdentityHashMap<View, Transaction>();

    private AutoCompleteTextView payeeText;
    private SimpleCursorAdapter payeeAdapter;
	private TextView differenceText;
	private boolean isUpdateBalanceMode = false;
    private boolean isShowPayee = true;
	private long currentBalance;
	private Utils u;

    private LinearLayout splitsLayout;
    private TextView unsplitAmountText;

    private QuickActionWidget unsplitActionGrid;

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
        prepareUnsplitActionGrid();
	}

    private void prepareUnsplitActionGrid() {
        if (isSupportedApiLevel()) {
            unsplitActionGrid = new QuickActionGrid(this);
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_input_add, R.string.transaction));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_input_transfer, R.string.transfer));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.gd_action_bar_share, R.string.unsplit_adjust_amount));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.gd_action_bar_share, R.string.unsplit_adjust_evenly));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.gd_action_bar_share, R.string.unsplit_adjust_last));
            unsplitActionGrid.setOnQuickActionClickListener(unsplitActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener unsplitActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    createSplit(false);
                    break;
                case 1:
                    createSplit(true);
                    break;
                case 2:
                    unsplitAdjustAmount();
                    break;
                case 3:
                    unsplitAdjustEvenly();
                    break;
                case 4:
                    unsplitAdjustLast();
                    break;
            }
        }

    };

    private void unsplitAdjustAmount() {
        long splitAmount = calculateSplitAmount();
        amountInput.setAmount(splitAmount);
        updateUnsplitAmount();
    }

    private void unsplitAdjustEvenly() {
        long unsplitAmount = calculateUnsplitAmount();
        if (unsplitAmount != 0) {
            List<Transaction> splits = new ArrayList<Transaction>(viewToSplitMap.values());
            SplitAdjuster.adjustEvenly(splits, unsplitAmount);
            updateSplits();
        }
    }

    private void unsplitAdjustLast() {
        long unsplitAmount = calculateUnsplitAmount();
        if (unsplitAmount != 0) {
            List<Transaction> splits = new ArrayList<Transaction>(viewToSplitMap.values());
            SplitAdjuster.adjustLast(splits, unsplitAmount);
            updateSplits();
        }
    }

    private void updateSplits() {
        for (Map.Entry<View, Transaction> entry : viewToSplitMap.entrySet()) {
            View v = entry.getKey();
            Transaction split = entry.getValue();
            setSplitData(v, split);
        }
        updateUnsplitAmount();
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
            payeeText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
                    if (isRememberLastCategory) {
                        selectLastCategoryForPayee(id);
                    }
                }
            });
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
		categoryText = x.addListNodeCategory(layout);
        categoryText.setText(R.string.no_category);
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

    private void selectLastCategoryForPayee(long id) {
        Payee p = em.get(Payee.class, id);
        if (p != null) {
            selectCategory(p.lastCategoryId, true);
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
            View v = x.addNodeUnsplit(splitsLayout);
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
        for (Transaction split : viewToSplitMap.values()) {
            amount += split.fromAmount;
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
        List<Transaction> splits = em.getSplitsForTransaction(transaction.id);
        for (Transaction split : splits) {
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
        if (isSplit(selectedCategoryId)) {
            transaction.splits = new LinkedList<Transaction>(viewToSplitMap.values());
        } else {
            transaction.splits = null;
        }
	}

    private void selectPayee(long payeeId) {
        if (isShowPayee) {
            Payee p = db.em().get(Payee.class, payeeId);
            selectPayee(p);
        }
    }

    private void selectPayee(Payee p) {
        if (p != null) {
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
            case R.id.unsplit_action:
                if (isSupportedApiLevel()) {
                    unsplitActionGrid.show(v);
                } else {
                    showQuickActionsDialog();
                }
                break;
            case R.id.add_split:
                createSplit(false);
                break;
            case R.id.add_split_transfer:
                createSplit(true);
                break;
            case R.id.delete_split:
                View parentView = (View)v.getParent();
                deleteSplit(parentView);
                break;
            case R.id.category_split:
                selectCategory(Category.SPLIT_CATEGORY_ID);
                break;
        }
        Transaction split = viewToSplitMap.get(v);
        if (split != null) {
            split.unsplitAmount = split.fromAmount + calculateUnsplitAmount();
            editSplit(split, split.toAccountId > 0 ? SplitTransferActivity.class : SplitTransactionActivity.class);
        }
    }

    private void showQuickActionsDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.unsplit_amount)
            .setItems(R.array.unsplit_quick_action_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    unsplitActionListener.onQuickActionClicked(unsplitActionGrid, i);
                }
            })
            .show();
    }

    private void createSplit(boolean asTransfer) {
        Transaction split = new Transaction();
        split.id = idSequence.decrementAndGet();
        split.fromAccountId = selectedAccountId;
        split.fromAmount = split.unsplitAmount = calculateUnsplitAmount();
        editSplit(split, asTransfer ? SplitTransferActivity.class : SplitTransactionActivity.class);
    }

    private void editSplit(Transaction split, Class splitActivityClass) {
        Intent intent = new Intent(this, splitActivityClass);
        split.toIntentAsSplit(intent);
        startActivityForResult(intent, SPLIT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPLIT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Transaction split = Transaction.fromIntentAsSplit(data);
                addOrEditSplit(split);
            }
        }
    }

    private void addOrEditSplit(Transaction split) {
        View v = findView(split);
        if (v  == null) {
            v = x.addSplitNodeMinus(splitsLayout, R.id.edit_aplit, R.id.delete_split, R.string.split, "");
        }
        setSplitData(v, split);
        viewToSplitMap.put(v, split);
        updateUnsplitAmount();
    }

    private View findView(Transaction split) {
        for (Map.Entry<View, Transaction> entry : viewToSplitMap.entrySet()) {
            Transaction s = entry.getValue();
            if (s.id == split.id) {
                return  entry.getKey();
            }
        }
        return null;
    }

    private void setSplitData(View v, Transaction split) {
        TextView label = (TextView)v.findViewById(R.id.label);
        TextView data = (TextView)v.findViewById(R.id.data);
        setSplitData(split, label, data);
    }

    private void setSplitData(Transaction split, TextView label, TextView data) {
        if (split.isTransfer()) {
            setSplitDataTransfer(split, label, data);
        } else {
            setSplitDataTransaction(split, label, data);
        }
    }

    private void setSplitDataTransaction(Transaction split, TextView label, TextView data) {
        label.setText(createSplitTransactionTitle(split));
        Currency currency = amountInput.getCurrency();
        u.setAmountText(data, currency, split.fromAmount, false);
    }

    private String createSplitTransactionTitle(Transaction split) {
        StringBuilder sb = new StringBuilder();
        Category category = db.getCategory(split.categoryId);
        sb.append(category.title);
        if (isNotEmpty(split.note)) {
            sb.append(" (").append(split.note).append(")");
        }
        return sb.toString();
    }

    private void setSplitDataTransfer(Transaction split, TextView label, TextView data) {
        Account fromAccount = em.getAccount(split.fromAccountId);
        Account toAccount = em.getAccount(split.toAccountId);
        u.setTransferTitleText(label, fromAccount, toAccount);
        u.setTransferAmountText(data, fromAccount.currency, split.fromAmount, toAccount.currency, split.toAmount);
    }

    private void deleteSplit(View v) {
        Transaction split = viewToSplitMap.remove(v);
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
