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
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.AmountInput.OnAmountChangedListener;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class TransactionActivity extends AbstractTransactionActivity {
	
	public static final String CURRENT_BALANCE_EXTRA = "accountCurrentBalance";

	private static final int MENU_TURN_GPS_ON = Menu.FIRST;
	
	private ToggleButton incomeExpenseButton;
	private TextView differenceText; 
	private boolean isUpdateBalanceMode = false;
	private long currentBalance;
	private Utils u;
	
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
	protected void createListNodes(LinearLayout layout) {		
		incomeExpenseButton = (ToggleButton)findViewById(R.id.bIncomeExpense);
		incomeExpenseButton.setChecked(false);		
		//account
		accountText = x.addListNode(layout, R.id.account, R.string.account, R.string.select_account);
		//amount
		amountInput = new AmountInput(this);
		amountInput.setOwner(this);
		x.addEditNode(layout, isUpdateBalanceMode ? R.string.new_balance : R.string.amount, amountInput);
		//category
		categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
		// difference
		if (isUpdateBalanceMode) {
			differenceText = x.addInfoNode(layout, -1, R.string.difference, "0");
			incomeExpenseButton.setEnabled(false);
			amountInput.setAmount(currentBalance);
			amountInput.setOnAmountChangedListener(new OnAmountChangedListener(){
				@Override
				public void onAmountChanged(long oldAmount, long newAmount) {
					long balanceDifference = newAmount-currentBalance;
					u.setAmountText(differenceText, amountInput.getCurrency(), balanceDifference, true);
					incomeExpenseButton.setChecked(balanceDifference > 0);
				}
			});
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
		if (checkSelectedId(selectedAccountId, R.string.select_account)) {
			updateTransactionFromUI();
			return true;
		}
		return false;
	}

	@Override
	protected void editTransaction(Transaction transaction) {
		super.editTransaction(transaction);
		selectAccount(transaction.fromAccountId, false);
		amountInput.setAmount(transaction.fromAmount);
		incomeExpenseButton.setChecked(transaction.fromAmount >= 0);		
	}

	private void updateTransactionFromUI() {
		updateTransactionFromUI(transaction);
		transaction.fromAccountId = selectedAccountId;
		long amount = amountInput.getAmount();
		if (isUpdateBalanceMode) {
			amount = Math.abs(amount - currentBalance);
		}
		transaction.fromAmount = incomeExpenseButton.isChecked() ? amount : -amount;
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

}
