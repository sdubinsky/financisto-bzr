/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.model.rates.ExchangeRate;
import ru.orangesoftware.financisto.model.rates.ExchangeRateProvider;
import ru.orangesoftware.financisto.utils.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static ru.orangesoftware.financisto.activity.ExchangeRateActivity.formatRateDate;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public abstract class AbstractTotalsDetailsActivity extends AbstractActivity {

    private LinearLayout layout;
    private View calculatingNode;
    private Utils u;
    protected boolean shouldShowHomeCurrencyTotal = true;

    private final int titleNodeResId;

    protected AbstractTotalsDetailsActivity(int titleNodeResId) {
        this.titleNodeResId = titleNodeResId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.totals_details);

        u = new Utils(this);
        layout = (LinearLayout)findViewById(R.id.list);
        calculatingNode = x.addTitleNodeNoDivider(layout, R.string.calculating);

        Button bOk = (Button)findViewById(R.id.bOK);
        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        internalOnCreate();
        calculateTotals();
    }

    protected void internalOnCreate() {}

    private void calculateTotals() {
        CalculateAccountsTotalsTask task = new CalculateAccountsTotalsTask();
        task.execute();
    }

    @Override
    protected void onClick(View v, int id) {
    }
    
    private class CalculateAccountsTotalsTask extends AsyncTask<Void, Void, TotalsInfo> {

        private final DecimalFormat nf = new DecimalFormat("0.00000");

        @Override
        protected TotalsInfo doInBackground(Void... voids) {
            Total[] totals = getTotals();
            Total totalInHomeCurrency = getTotalInHomeCurrency();
            Currency homeCurrency = totalInHomeCurrency.currency;
            ExchangeRateProvider rates = db.getLatestRates();
            List<TotalInfo> result = new ArrayList<TotalInfo>();
            for (Total total : totals) {
                ExchangeRate rate = rates.getRate(total.currency, homeCurrency);
                TotalInfo info = new TotalInfo(total, rate);
                result.add(info);
            }
            return new TotalsInfo(result, totalInHomeCurrency);
        }

        @Override
        protected void onPostExecute(TotalsInfo totals) {
            calculatingNode.setVisibility(View.GONE);
            Currency homeCurrency = totals.totalInHomeCurrency.currency;
            for (TotalInfo total : totals.totals) {
                addAmountNode(total, homeCurrency);
            }
            if (shouldShowHomeCurrencyTotal) {
                addHomeCurrencyAmountNode(totals.totalInHomeCurrency);
            }
        }

        private void addAmountNode(TotalInfo total, Currency homeCurrency) {
            String title = getString(titleNodeResId, total.total.currency.name);
            x.addTitleNodeNoDivider(layout, title);
            TextView data = addAmountNode(total.total);
            String rateInfo = new StringBuilder().append("1").append(total.total.currency).append("=")
                    .append(nf.format(total.rate.rate)).append(homeCurrency)
                    .append(" (").append(formatRateDate(AbstractTotalsDetailsActivity.this, total.rate.date)).append(")").toString();
            data.setText(rateInfo);
        }

        private void addHomeCurrencyAmountNode(Total total) {
            x.addTitleNodeNoDivider(layout, getString(R.string.home_currency_total, total.currency.name));
            TextView data = addAmountNode(total);
            data.setText(R.string.home_currency);
        }

        private TextView addAmountNode(Total total) {
            TextView data = x.addInfoNode(layout, -1, "", "");
            View v = (View) data.getTag();
            TextView label = (TextView)v.findViewById(R.id.label);
            u.setAmountText(label, total.currency, total.balance, false);
            return data;
        }
    }

    protected abstract Total getTotalInHomeCurrency();

    protected abstract Total[] getTotals();

    private static class TotalInfo {

        public final Total total;
        public final ExchangeRate rate;

        public TotalInfo(Total total, ExchangeRate rate) {
            this.total = total;
            this.rate = rate;
        }
    }
    
    private static class TotalsInfo {
        
        public final List<TotalInfo> totals;
        public final Total totalInHomeCurrency;

        public TotalsInfo(List<TotalInfo> totals, Total totalInHomeCurrency) {
            this.totals = totals;
            this.totalInHomeCurrency = totalInHomeCurrency;
        }

    }
    

}
