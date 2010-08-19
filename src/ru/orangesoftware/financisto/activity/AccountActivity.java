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
import ru.orangesoftware.financisto.adapter.AccountTypeAdapter;
import ru.orangesoftware.financisto.adapter.CardIssuerAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.CardIssuer;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.TransactionUtils;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.orb.EntityManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class AccountActivity extends AbstractActivity {
	
	public static final String ACCOUNT_ID_EXTRA = "accountId";
	
	private static final int NEW_CURRENCY_REQUEST = 1;
	
	private int negativeAmountColor; 
	
	private AmountInput amountInput;
	private AmountInput limitInput;
	private View limitAmountView;
	private EditText accountTitle;

	private Cursor currencyCursor;
	private TextView currencyText;
	private View accountTypeNode;
	private View cardIssuerNode;
	private View issuerNode;
	private EditText numberText;
	private View numberNode;
	private EditText issuerName;
	private EditText sortOrderText;
	private CheckBox isIncludedIntoTotals;
	private CheckBox isNegativeOpeningAmount;
	
	private AccountTypeAdapter accountTypeAdapter;
	private CardIssuerAdapter cardIssuerAdapter;
	private ListAdapter currencyAdapter;	
	
	private Account account = new Account();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account);
		
		negativeAmountColor = getResources().getColor(R.color.negative_amount);
		
		accountTitle = new EditText(this);
		accountTitle.setSingleLine();
		
		issuerName = new EditText(this);
		issuerName.setSingleLine();

		numberText = new EditText(this);
		numberText.setHint(R.string.card_number_hint);
		numberText.setSingleLine();

		sortOrderText = new EditText(this);
		sortOrderText.setInputType(InputType.TYPE_CLASS_NUMBER);
		sortOrderText.setSingleLine();
		
		amountInput = new AmountInput(this);
		amountInput.setOwner(this);

		limitInput = new AmountInput(this);
		limitInput.setOwner(this);

		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);		

		accountTypeAdapter = new AccountTypeAdapter(this);
		accountTypeNode = x.addListNodeIcon(layout, R.id.account_type, R.string.account_type, R.string.account_type);
		
		cardIssuerAdapter = new CardIssuerAdapter(this);
		cardIssuerNode = x.addListNodeIcon(layout, R.id.card_issuer, R.string.card_issuer, R.string.card_issuer);
		setVisibility(cardIssuerNode, View.GONE);
		
		issuerNode = x.addEditNode(layout, R.string.issuer, issuerName);
		setVisibility(issuerNode, View.GONE);
		
		numberNode = x.addEditNode(layout, R.string.card_number, numberText);
		setVisibility(numberNode, View.GONE);

		currencyCursor = em.getAllCurrencies("name");
		startManagingCursor(currencyCursor);		
		currencyAdapter = TransactionUtils.createCurrencyAdapter(this, currencyCursor);

		x.addEditNode(layout, R.string.title, accountTitle);		
		currencyText = x.addListNodePlus(layout, R.id.currency, R.id.currency_add, R.string.currency, R.string.select_currency);
		
		limitAmountView = x.addEditNode(layout, R.string.limit_amount, limitInput);
		setVisibility(limitAmountView, View.GONE);

		Intent intent = getIntent();
		if (intent != null) {
			long accountId = intent.getLongExtra(ACCOUNT_ID_EXTRA, -1);
			if (accountId != -1) {
				this.account = em.getAccount(accountId);
				if (this.account == null) {
					this.account = new Account();
				}
			} else {
				selectAccountType(AccountType.valueOf(account.type));
			}
		}

		if (account.id == -1) {
			x.addEditNode(layout, R.string.opening_amount, amountInput);
			isNegativeOpeningAmount = x.addCheckboxNode(layout, R.id.negative_opening_amount, 
					R.string.negative_opening_amount, R.string.negative_opening_amount_summary, false);
		}
		
		x.addEditNode(layout, R.string.sort_order, sortOrderText);
		isIncludedIntoTotals = x.addCheckboxNode(layout,
				R.id.is_included_into_totals, R.string.is_included_into_totals,
				R.string.is_included_into_totals_summary, true);
		
		if (account.id > 0) {
			editAccount();
		}

		Button bOK = (Button)findViewById(R.id.bOK);
		bOK.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (account.currency == null) {
					Toast.makeText(AccountActivity.this, R.string.select_currency, Toast.LENGTH_SHORT).show();
					return;	
				}
				if (Utils.isEmpty(accountTitle)) {
					accountTitle.setError(getString(R.string.title));
					return;
				}
				AccountType type = AccountType.valueOf(account.type);
				if (type.hasIssuer) {
					account.issuer = Utils.text(issuerName);
				}
				if (type.hasNumber) {
					account.number = Utils.text(numberText);
				}
				account.title = accountTitle.getText().toString();
				account.creationDate = System.currentTimeMillis();
				String sortOrder = Utils.text(sortOrderText);
				account.sortOrder = sortOrder == null ? 0 : Integer.parseInt(sortOrder);
				account.isIncludeIntoTotals  = isIncludedIntoTotals.isChecked();
				account.limitAmount = limitInput.getAmount();
				long accountId = em.saveAccount(account);
				long amount = amountInput.getAmount();
				if (amount != 0) {
					Transaction t = new Transaction();
					t.fromAccountId = accountId;
					t.categoryId = 0;
					t.note = getResources().getText(R.string.opening_amount) + " (" +account.title + ")";
					t.fromAmount = isNegativeOpeningAmount.isChecked() ? -amount : amount;
					db.insertOrUpdate(t, null);
				}
				Intent intent = new Intent();
				intent.putExtra(ACCOUNT_ID_EXTRA, accountId);
				setResult(RESULT_OK, intent);
				finish();
			}

		});

		Button bCancel = (Button)findViewById(R.id.bCancel);
		bCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}			
		});
		
	}	
	
	@Override
	protected void onClick(View v, int id) {
		switch(id) {
			case R.id.is_included_into_totals:
				isIncludedIntoTotals.performClick();
				break;
			case R.id.negative_opening_amount:
				isNegativeOpeningAmount.performClick();
				changeColorOfTheAmountInput();
				break;
			case R.id.account_type:				
				x.selectPosition(this, R.id.account_type, R.string.account_type, accountTypeAdapter, AccountType.valueOf(account.type).ordinal());
				break;
			case R.id.card_issuer:				
				x.selectPosition(this, R.id.card_issuer, R.string.card_issuer, cardIssuerAdapter, 
						account.cardIssuer != null ? CardIssuer.valueOf(account.cardIssuer).ordinal() : 0);
				break;
			case R.id.currency:				
				x.select(this, R.id.currency, R.string.currency, currencyCursor, currencyAdapter, 
						"_id", account.currency != null ? account.currency.id : -1);
				break;
			case R.id.currency_add:
				Intent intent = new Intent(AccountActivity.this, CurrencyActivity.class);
				startActivityForResult(intent, NEW_CURRENCY_REQUEST);
				break;
		}
	}	

	private void changeColorOfTheAmountInput() {
		amountInput.setColor(isNegativeOpeningAmount.isChecked() ? negativeAmountColor : Color.BLACK);
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		switch(id) {
			case R.id.currency:
				selectCurrency(selectedId);
				break;
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch(id) {
			case R.id.account_type:
				AccountType type = AccountType.values()[selectedPos];
				selectAccountType(type);
				break;
			case R.id.card_issuer:
				CardIssuer issuer = CardIssuer.values()[selectedPos];
				selectCardIssuer(issuer);
				break;
		}
	}

	private void selectAccountType(AccountType type) {
		ImageView icon = (ImageView)accountTypeNode.findViewById(R.id.icon);
		icon.setImageResource(type.iconId);
		TextView label = (TextView)accountTypeNode.findViewById(R.id.label);
		label.setText(type.titleId);
//		TextView data = (TextView)accountTypeNode.findViewById(R.id.data);
//		if (type.subTitleId > 0) {
//			data.setText(type.subTitleId);
//			data.setVisibility(View.VISIBLE);
//		} else {
//			data.setVisibility(View.GONE);
//		}
		setVisibility(cardIssuerNode, type.isCard ? View.VISIBLE : View.GONE);
		setVisibility(issuerNode, type.hasIssuer ? View.VISIBLE : View.GONE);
		setVisibility(numberNode, type.hasNumber ? View.VISIBLE : View.GONE);
		setVisibility(limitAmountView, type == AccountType.CREDIT_CARD ? View.VISIBLE : View.GONE);
		account.type = type.name();
		selectCardIssuer(account.cardIssuer != null 
				? CardIssuer.valueOf(account.cardIssuer)
				: CardIssuer.VISA);
	}

	private void selectCardIssuer(CardIssuer issuer) {
		ImageView icon = (ImageView)cardIssuerNode.findViewById(R.id.icon);
		icon.setImageResource(issuer.iconId);
		TextView label = (TextView)cardIssuerNode.findViewById(R.id.label);
		label.setText(issuer.titleId);
		account.cardIssuer = issuer.name();
	}

	private void selectCurrency(long currencyId) {
		if (Utils.moveCursor(currencyCursor, "_id", currencyId) != -1) {
			Currency c = EntityManager.loadFromCursor(currencyCursor, Currency.class);
			selectCurrency(c);
		}
	}
	
	private void selectCurrency(Currency c) {
		currencyText.setText(c.name);						
		amountInput.setCurrency(c);
		account.currency = c;		
	}

	private void editAccount() {
		selectAccountType(AccountType.valueOf(account.type));
		if (account.cardIssuer != null) {
			selectCardIssuer(CardIssuer.valueOf(account.cardIssuer));
		}
		selectCurrency(account.currency);
		accountTitle.setText(account.title);
		issuerName.setText(account.issuer);
		numberText.setText(account.number);
		sortOrderText.setText(String.valueOf(account.sortOrder));
		isIncludedIntoTotals.setChecked(account.isIncludeIntoTotals);
		if (account.limitAmount > 0) {
			limitInput.setAmount(account.limitAmount);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (amountInput.processActivityResult(requestCode, data)) {
				return;
			}
			if (limitInput.processActivityResult(requestCode, data)) {
				return;
			}
			switch(requestCode) {
			case NEW_CURRENCY_REQUEST:
				currencyCursor.requery();
				long currencyId = data.getLongExtra(CurrencyActivity.CURRENCY_ID_EXTRA, -1);
				if (currencyId != -1) {
					selectCurrency(currencyId);
				}
				break;
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}	
		
}
