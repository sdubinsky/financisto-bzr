/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.GenericViewHolder;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.rates.ExchangeRate;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static ru.orangesoftware.financisto.activity.ExchangeRateActivity.formatRateDate;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/18/12 11:10 PM
 */
public class ExchangeRatesListActivity extends AbstractListActivity {

    private static final int ADD_RATE = 1;
    private static final int EDIT_RATE = 1;

    private Spinner fromCurrencySpinner;
    private Spinner toCurrencySpinner;
    private List<Currency> currencies;

    private long lastSelectedCurrencyId;

    public ExchangeRatesListActivity() {
        super(R.layout.exchange_rate_list);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        super.internalOnCreate(savedInstanceState);
        currencies = em.getAllCurrenciesList("name");

        fromCurrencySpinner = (Spinner) findViewById(R.id.spinnerFromCurrency);
        fromCurrencySpinner.setPromptId(R.string.rate_from_currency);
        toCurrencySpinner = (Spinner) findViewById(R.id.spinnerToCurrency);
        toCurrencySpinner.setPromptId(R.string.rate_to_currency);

        if (currencies.size() > 0) {
            toCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    updateAdapter();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            fromCurrencySpinner.setAdapter(createCurrencyAdapter(currencies));
            fromCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                    List<Currency> currencies = getCurrenciesButSelected(id);
                    if (currencies.size() > 0) {
                        int position = findSelectedCurrency(currencies, lastSelectedCurrencyId);
                        toCurrencySpinner.setAdapter(createCurrencyAdapter(currencies));
                        toCurrencySpinner.setSelection(position);
                    }
                    lastSelectedCurrencyId = id;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            fromCurrencySpinner.setSelection(findDefaultCurrency());

            ImageButton bFlip = (ImageButton)findViewById(R.id.bFlip);
            bFlip.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    flipCurrencies();
                }
            });
        }
    }

    private SpinnerAdapter createCurrencyAdapter(List<Currency> currencies) {
        ArrayAdapter<Currency> a = new ArrayAdapter<Currency>(this, android.R.layout.simple_spinner_item, currencies){
            @Override
            public long getItemId(int position) {
                return getItem(position).id;
            }
        };
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private List<Currency> getCurrenciesButSelected(long id) {
        List<Currency> list = new ArrayList<Currency>();
        for (Currency currency : currencies) {
            if (currency.id != id) {
                list.add(currency);
            }
        }
        return list;
    }

    private int findSelectedCurrency(List<Currency> currencies, long id) {
        int i = 0;
        for (Currency currency : currencies) {
            if (currency.id == id) {
                return i;
            }
            ++i;
        }
        return 0;
    }

    private int findDefaultCurrency() {
        int i = 0;
        for (Currency currency : currencies) {
            if (currency.isDefault) {
                return i;
            }
            ++i;
        }
        return 0;
    }

    private void flipCurrencies() {
        Currency toCurrency = (Currency) toCurrencySpinner.getSelectedItem();
        if (toCurrency != null) {
            fromCurrencySpinner.setSelection(findSelectedCurrency(currencies, toCurrency.id));
        }
    }

    private void updateAdapter() {
        Currency fromCurrency = (Currency) fromCurrencySpinner.getSelectedItem();
        Currency toCurrency = (Currency) toCurrencySpinner.getSelectedItem();
        List<ExchangeRate> rates = db.findRates(fromCurrency, toCurrency);
        ListAdapter adapter = new ExchangeRateListAdapter(this, rates);
        setListAdapter(adapter);
    }

    @Override
    protected void addItem() {
        long fromCurrencyId = fromCurrencySpinner.getSelectedItemId();
        long toCurrencyId = toCurrencySpinner.getSelectedItemId();
        if (fromCurrencyId > 0 && toCurrencyId > 0) {
            Intent intent = new Intent(this, ExchangeRateActivity.class);
            intent.putExtra(ExchangeRateActivity.FROM_CURRENCY_ID, fromCurrencyId);
            intent.putExtra(ExchangeRateActivity.TO_CURRENCY_ID, toCurrencyId);
            startActivityForResult(intent, ADD_RATE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            updateAdapter();
        }
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return null;
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        ExchangeRate rate = (ExchangeRate) getListAdapter().getItem(position);
        db.deleteRate(rate);
        updateAdapter();
    }

    @Override
    protected void editItem(View v, int position, long id) {
        ExchangeRate rate = (ExchangeRate) getListAdapter().getItem(position);
        editRate(rate);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        editItem(v, position, id);
    }

    private void editRate(ExchangeRate rate) {
        Intent intent = new Intent(this, ExchangeRateActivity.class);
        intent.putExtra(ExchangeRateActivity.FROM_CURRENCY_ID, rate.fromCurrencyId);
        intent.putExtra(ExchangeRateActivity.TO_CURRENCY_ID, rate.toCurrencyId);
        intent.putExtra(ExchangeRateActivity.RATE_DATE, rate.date);
        startActivityForResult(intent, EDIT_RATE);
    }

    private static class ExchangeRateListAdapter extends BaseAdapter {

        private final StringBuilder sb = new StringBuilder();
        private final DecimalFormat nf = new DecimalFormat("0.00000");

        private final Context context;
        private final LayoutInflater inflater;
        private final List<ExchangeRate> rates;

        private ExchangeRateListAdapter(Context context, List<ExchangeRate> rates) {
            this.context = context;
            this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.rates = rates;
        }

        @Override
        public int getCount() {
            return rates.size();
        }

        @Override
        public ExchangeRate getItem(int i) {
            return rates.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GenericViewHolder v;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.generic_list_item, parent, false);
                v = GenericViewHolder.createAndTag(convertView);
            } else {
                v = (GenericViewHolder)convertView.getTag();
            }
            ExchangeRate rate = getItem(position);
            v.lineView.setText(formatRateDate(context, rate.date));
            v.amountView.setText(nf.format(rate.rate));
            return convertView;
        }
    }

}
