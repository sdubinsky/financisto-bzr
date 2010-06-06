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

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.widget.AmountInput;
import ru.orangesoftware.financisto.widget.CalculatorInput;
import ru.orangesoftware.financisto.widget.AmountInput.OnAmountChangedListener;
import ru.orangesoftware.orb.EntityManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TransferActivity extends AbstractTransactionActivity {

	private static final int EDIT_RATE = 1;
	
	private AmountInput amountInputFrom;
	private View rateInfoNode;
	private TextView rateInfo;
	private EditText rate;
	private ImageButton bCalc;
	private ImageButton bDownload;
	private View amountInputToNode;
	private AmountInput amountInputTo;
	private TextView accountFromText;
	private TextView accountToText;	
	
	private long selectedAccountFromId = -1;
	private long selectedAccountToId = -1;
	
	private double lastRate = 0.0;
		
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

	protected int getLayoutId() {
		return MyPreferences.isUseFixedLayout(this) ? R.layout.transfer_fixed : R.layout.transfer_free;
	}
	
	@Override
	protected void editTransaction(Transaction transaction) {
		super.editTransaction(transaction);
		amountInputFrom.setAmount(transaction.fromAmount);
		selectAccount(accountFromText, amountInputFrom, transaction.fromAccountId, false);
		selectedAccountFromId = transaction.fromAccountId;
		amountInputTo.setAmount(transaction.toAmount);
		selectAccount(accountToText, amountInputTo, transaction.toAccountId, false);
		selectedAccountToId = transaction.toAccountId;
		checkNeedRate();
	}

	private final OnAmountChangedListener onAmountFromChangedListener = new OnAmountChangedListener(){
		@Override
		public void onAmountChanged(long oldAmount, long newAmount) {
			double r = getRate();
			if (r > 0) {
				long amountFrom = amountInputFrom.getAmount();
				long amountTo = (long)Math.round(r*amountFrom);
				amountInputTo.setOnAmountChangedListener(null);
				amountInputTo.setAmount(amountTo);
				amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
			} else {
				long amountFrom = amountInputFrom.getAmount();
				long amountTo = amountInputTo.getAmount();
				if (amountFrom > 0) {
					rate.removeTextChangedListener(rateWatcher);
					setRate(1.0f*amountTo/amountFrom);
					rate.addTextChangedListener(rateWatcher);					
				}
			}
			setRateInfo();
		}
	};
	
	private final OnAmountChangedListener onAmountToChangedListener = new OnAmountChangedListener(){
		@Override
		public void onAmountChanged(long oldAmount, long newAmount) {
			rate.removeTextChangedListener(rateWatcher);
			long amountFrom = amountInputFrom.getAmount();
			long amountTo = amountInputTo.getAmount();
			if (amountFrom > 0) {
				rate.removeTextChangedListener(rateWatcher);
				setRate(1.0d*amountTo/amountFrom);
				rate.addTextChangedListener(rateWatcher);					
			}
			setRateInfo();
			rate.addTextChangedListener(rateWatcher);
		}
	};

	@Override
	protected void createListNodes(LinearLayout layout) {
		accountFromText = x.addListNode(layout, R.id.account_from, R.string.account_from, R.string.select_account);
		accountToText = x.addListNode(layout, R.id.account_to, R.string.account_to, R.string.select_account);
		//amount from
		amountInputFrom = new AmountInput(this);
		amountInputFrom.setOwner(this);
		amountInputFrom.setOnAmountChangedListener(onAmountFromChangedListener);
		x.addEditNode(layout, R.string.amount_from, amountInputFrom);
		//amount to & rate 
		amountInputTo = new AmountInput(this);
		amountInputTo.setOwner(this);
		amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
		amountInputToNode = x.addEditNode(layout, R.string.amount_to, amountInputTo);
		setVisibility(amountInputToNode, View.GONE);
		rateInfoNode = addRateNode(layout);
		rate = (EditText)rateInfoNode.findViewById(R.id.rate);
		rate.addTextChangedListener(rateWatcher);
		rateInfo = (TextView)rateInfoNode.findViewById(R.id.data);
		bCalc = (ImageButton)rateInfoNode.findViewById(R.id.rateCalculator);
		bCalc.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TransferActivity.this, CalculatorInput.class);
				intent.putExtra(AmountInput.EXTRA_AMOUNT, Utils.text(rate));
				startActivityForResult(intent, EDIT_RATE);
			}			
		});
		bDownload = (ImageButton)rateInfoNode.findViewById(R.id.rateDownload);
		bDownload.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				new RateDownloadTask().execute(selectedCurrencyFromName, selectedCurrencyToName);
			}
		});
		setVisibility(rateInfoNode, View.GONE);
		//category
		categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
	}
	
	private final HttpClient httpClient = new DefaultHttpClient();
	private final Pattern pattern = Pattern.compile("<double.*?>(.+?)</double>"); 
	
	private class RateDownloadTask extends AsyncTask<String, Void, Double> {
		
		private String error;
		
		@Override
		protected Double doInBackground(String... args) {			
			HttpGet get = new HttpGet("http://www.webservicex.net/CurrencyConvertor.asmx/ConversionRate?FromCurrency="+args[0]+"&ToCurrency="+args[1]);			
			try {
				HttpResponse r = httpClient.execute(get);
				String s = EntityUtils.toString(r.getEntity());
				Log.i("RateDownload", s);
				Matcher m = pattern.matcher(s);
				if (m.find()) {
					String d = m.group(1);
					return Double.valueOf(d);
				} else {
					String[] x = s.split("\r\n");
					error = getString(R.string.service_is_not_available);
					if (x.length > 0) {
						error = x[0];
					}
				}
			} catch (Exception e) {
				error = e.getMessage();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			amountInputFrom.setEnabled(false);
			amountInputTo.setEnabled(false);
			rate.setEnabled(false);
			bCalc.setEnabled(false);
			bDownload.setEnabled(false);
		}
				
		@Override
		protected void onPostExecute(Double result) {
			setProgressBarIndeterminateVisibility(false);
			amountInputFrom.setEnabled(true);
			amountInputTo.setEnabled(true);
			rate.setEnabled(true);
			bCalc.setEnabled(true);
			bDownload.setEnabled(true);
			if (result == null) {
				if (error != null) {
					Toast t = Toast.makeText(TransferActivity.this, error, Toast.LENGTH_LONG);
					t.show();
				}
			} else {
				setRate(result);
			}
		}

	}
	
	private final TextWatcher rateWatcher = new TextWatcher(){
		@Override
		public void afterTextChanged(Editable s) {
			double r = getRate();				
			long amountFrom = amountInputFrom.getAmount();
			long amountTo = (long)Math.floor(r*amountFrom);
			amountInputTo.setOnAmountChangedListener(null);
			amountInputTo.setAmount(amountTo);
			setRateInfo();
			amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	};
	
	private View addRateNode(LinearLayout layout) {
		return x.inflater.new Builder(layout, R.layout.select_entry_rate)
		.withLabel(R.string.rate)
		.withData(R.string.no_rate)
		.create();
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
		if (isDifferentCurrencies()) {
			transaction.fromAmount = -amountInputFrom.getAmount();
			transaction.toAmount = amountInputTo.getAmount();
		} else {
			transaction.fromAmount = -amountInputFrom.getAmount();
			transaction.toAmount = amountInputFrom.getAmount();
		}
	}

	@Override
	protected void onClick(View v, int id) {
		super.onClick(v, id);
		switch (id) {
			case R.id.account_from:				
				x.select(R.id.account_from, R.string.account, accountCursor, accountAdapter, 
						AccountColumns.ID, selectedAccountFromId);
				break;
			case R.id.account_to:				
				x.select(R.id.account_to, R.string.account, accountCursor, accountAdapter, 
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
		if (selectAccount(accountFromText, amountInputFrom, selectedId, true)) {
			selectedAccountFromId = selectedId;
			checkNeedRate();
		}
	}
	
	private void selectToAccount(long selectedId) {
		if (selectAccount(accountToText, amountInputTo, selectedId, false)) {
			selectedAccountToId = selectedId;
			checkNeedRate();
		}
	}

	@Override
	protected void selectAccount(long accountId, boolean selectLast) {
		if (selectAccount(accountFromText, amountInputFrom, accountId, selectLast)) {
			selectedAccountFromId = accountId;
		}
	}

	protected boolean selectAccount(TextView accountText, AmountInput amountInput, long accountId, boolean selectLast) {
		if (Utils.moveCursor(accountCursor, AccountColumns.ID, accountId) != -1) {
			Account a = EntityManager.loadFromCursor(accountCursor, Account.class);
			Currency c = CurrencyCache.putCurrency(a.currency);
			accountText.setText(a.title);						
			amountInput.setCurrency(c);		
			if (selectLast && isRememberLastAccount) {
				selectedAccountFromId = accountId;
				selectToAccount(a.lastAccountId);
			}
			if (selectLast && isRememberLastCategory) {
				selectCategory(a.lastCategoryId, true);
			}
			return true;
		}
		return false;
	}
	
	private String selectedCurrencyFromName;
	private String selectedCurrencyToName;
	
	private boolean isDifferentCurrencies() {
		if (selectedAccountFromId > 0 && selectedAccountToId > 0) {
			if (Utils.moveCursor(accountCursor, AccountColumns.ID, selectedAccountFromId) != -1) {
				Account a1 = EntityManager.loadFromCursor(accountCursor, Account.class);
				long currencyFromId = a1.currency.id;
				if (Utils.moveCursor(accountCursor, AccountColumns.ID, selectedAccountToId) != -1) {
					Account a2 = EntityManager.loadFromCursor(accountCursor, Account.class);
					long currencyToId = a2.currency.id;
					return currencyFromId != currencyToId;
				}
			}
		}		
		return false;
	}

	private void checkNeedRate() {
		if (selectedAccountFromId > 0 && selectedAccountToId > 0) {
			if (Utils.moveCursor(accountCursor, AccountColumns.ID, selectedAccountFromId) != -1) {
				Account a1 = EntityManager.loadFromCursor(accountCursor, Account.class);
				long currencyFromId = a1.currency.id;
				selectedCurrencyFromName = a1.currency.name;
				if (Utils.moveCursor(accountCursor, AccountColumns.ID, selectedAccountToId) != -1) {
					Account a2 = EntityManager.loadFromCursor(accountCursor, Account.class);
					long currencyToId = a2.currency.id;
					selectedCurrencyToName = a2.currency.name;
					if (currencyFromId == currencyToId) {
						setVisibility(rateInfoNode, View.GONE);
						setVisibility(amountInputToNode, View.GONE);
					} else {
						setVisibility(rateInfoNode, View.VISIBLE);
						setVisibility(amountInputToNode, View.VISIBLE);		
						calculateRate();
					}		
				}
			}
		}
	}
	
	private final DecimalFormat nf = new DecimalFormat("0.0000");

	private void calculateRate() {
		if (Utils.isEmpty(rate)) {
			long amountFrom = amountInputFrom.getAmount();
			long amountTo = amountInputTo.getAmount();
			float r = 1.0f*amountTo/amountFrom;
			if (!Double.isNaN(r)) {
				rate.setText(nf.format(r));
			}
		}
		setRateInfo();
	}
	
	private void setRateInfo() {
		double r = getRate();
		StringBuilder sb = new StringBuilder();
		sb.append("1").append(selectedCurrencyFromName).append("=").append(nf.format(r)).append(selectedCurrencyToName).append(", ");
		sb.append("1").append(selectedCurrencyToName).append("=").append(nf.format(1.0/r)).append(selectedCurrencyFromName);
		rateInfo.setText(sb.toString());
	}
	
	protected double getRate() {
		return lastRate;
	}

	private void setRate(double r) {
		lastRate = r;
		rate.setText(nf.format(r));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (amountInputFrom.processActivityResult(requestCode, data)) {
				calculateRate();
				return;
			}
			if (amountInputTo.processActivityResult(requestCode, data)) {
				calculateRate();
				return;
			}
			if (requestCode == EDIT_RATE) {
				String amount = data.getStringExtra(AmountInput.EXTRA_AMOUNT);
				if (amount != null) {
					setRate(Float.parseFloat(amount));
				}
			}
		}
	}	

}
