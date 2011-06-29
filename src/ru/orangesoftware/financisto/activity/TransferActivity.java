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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.widget.RateLayoutView;

public class TransferActivity extends AbstractTransactionActivity {

    private RateLayoutView rateView;
    private TextView accountFromText;
    private TextView accountToText;

	private long selectedAccountFromId = -1;
	private long selectedAccountToId = -1;

	public TransferActivity() {
	}

	@Override
	protected void internalOnCreate() {
		super.internalOnCreate();
		if (transaction.isTemplateLike()) {
			setTitle(transaction.isTemplate() ? R.string.transfer_template : R.string.transfer_schedule);
			if (transaction.isTemplate()) {			
				dateText.setEnabled(false);
				timeText.setEnabled(false);			
			}			
		}
	}

    protected Cursor fetchCategories() {
        return db.getCategories(true);
    }

	protected int getLayoutId() {
		return MyPreferences.isUseFixedLayout(this) ? R.layout.transfer_fixed : R.layout.transfer_free;
	}
	
	@Override
	protected void createListNodes(LinearLayout layout) {
        accountFromText = x.addListNode(layout, R.id.account_from, R.string.account_from, R.string.select_account);
        accountToText = x.addListNode(layout, R.id.account_to, R.string.account_to, R.string.select_account);
        // amounts
        rateView = new RateLayoutView(this, x, layout);
        rateView.createUI();
		//category
		categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
	}
	
    @Override
    protected void editTransaction(Transaction transaction) {
        if (transaction.fromAccountId > 0) {
            Account fromAccount = em.getAccount(transaction.fromAccountId);
            selectAccount(fromAccount, accountFromText, false);
            rateView.selectFromAccount(fromAccount);
            rateView.setFromAmount(transaction.fromAmount);
            selectedAccountFromId = transaction.fromAccountId;
        }
        commonEditTransaction(transaction);
        if (transaction.toAccountId > 0) {
            Account toAccount = em.getAccount(transaction.toAccountId);
            selectAccount(toAccount, accountToText, false);
            rateView.selectToAccount(toAccount);
            rateView.setToAmount(transaction.toAmount);
            selectedAccountToId = transaction.toAccountId;
        }
    }

	@Override
	protected boolean onOKClicked() {
		if (selectedAccountFromId == -1) {
			Toast.makeText(this, R.string.select_from_account, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (selectedAccountToId == -1) {
			Toast.makeText(this, R.string.select_to_account, Toast.LENGTH_SHORT).show();
			return false;
		}
		updateTransferFromUI();
		return true;
	}

	private void updateTransferFromUI() {
		updateTransactionFromUI(transaction);
		transaction.fromAccountId = selectedAccountFromId;
		transaction.toAccountId = selectedAccountToId;
        transaction.fromAmount = rateView.getFromAmount();
        transaction.toAmount = rateView.getToAmount();
	}

	@Override
	protected void onClick(View v, int id) {
		super.onClick(v, id);
		switch (id) {
			case R.id.account_from:				
				x.select(this, R.id.account_from, R.string.account, accountCursor, accountAdapter, 
						AccountColumns.ID, selectedAccountFromId);
				break;
			case R.id.account_to:				
				x.select(this, R.id.account_to, R.string.account, accountCursor, accountAdapter, 
						AccountColumns.ID, selectedAccountToId);
				break;
		}
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		super.onSelectedId(id, selectedId);
		switch (id) {
			case R.id.account_from:		
				selectFromAccount(selectedId);
				break;
			case R.id.account_to:
				selectToAccount(selectedId);
				break;
		}
	}
	
	private void selectFromAccount(long selectedId) {
        selectAccount(selectedId, true);
	}
	
	private void selectToAccount(long selectedId) {
        Account account = em.getAccount(selectedId);
        if (account != null) {
            selectAccount(account, accountToText, false);
            selectedAccountToId = selectedId;
            rateView.selectToAccount(account);
        }
	}

	@Override
	protected void selectAccount(long accountId, boolean selectLast) {
        Account account = em.getAccount(accountId);
        if (account != null) {
            selectAccount(account, accountFromText, selectLast);
            selectedAccountFromId = accountId;
            rateView.selectFromAccount(account);
        }
	}

	protected void selectAccount(Account account, TextView accountText, boolean selectLast) {
        accountText.setText(account.title);
        if (selectLast && isRememberLastAccount) {
            selectToAccount(account.lastAccountId);
        }
        if (selectLast && isRememberLastCategory) {
            selectCategory(account.lastCategoryId, true);
        }
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
            rateView.onActivityResult(requestCode, data);
		}
	}	

}
