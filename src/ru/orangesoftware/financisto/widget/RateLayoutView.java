package ru.orangesoftware.financisto.widget;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractActivity;
import ru.orangesoftware.financisto.activity.ActivityLayout;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.utils.Utils;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.orangesoftware.financisto.activity.AbstractActivity.setVisibility;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 6/24/11 6:45 PM
 */
public class RateLayoutView {

    private static final int EDIT_RATE = 1;

    private final DecimalFormat nf = new DecimalFormat("0.0000");
    private final AbstractActivity activity;
    private final ActivityLayout x;
    private final LinearLayout layout;

    private AmountInput amountInputFrom;
    private AmountInput amountInputTo;

    private View rateInfoNode;
    private TextView rateInfo;
    private EditText rate;
    private ImageButton bCalc;
    private ImageButton bDownload;
    private View amountInputToNode;

    private AmountInput.OnAmountChangedListener amountFromChangeListener;
    private AmountInput.OnAmountChangedListener amountToChangeListener;
    private Account selectedAccountFrom;
    private Account selectedAccountTo;

    public RateLayoutView(AbstractActivity activity, ActivityLayout x, LinearLayout layout) {
        this.activity = activity;
        this.x = x;
        this.layout = layout;
    }

    public void setAmountFromChangeListener(AmountInput.OnAmountChangedListener amountFromChangeListener) {
        this.amountFromChangeListener = amountFromChangeListener;
    }

    public void setAmountToChangeListener(AmountInput.OnAmountChangedListener amountToChangeListener) {
        this.amountToChangeListener = amountToChangeListener;
    }

    public void createUI() {
        //amount from
        amountInputFrom = new AmountInput(activity);
        amountInputFrom.setOwner(activity);
        amountInputFrom.setExpense();
        amountInputFrom.disableIncomeExpenseButton();
        x.addEditNode(layout, R.string.amount_from, amountInputFrom);
        //amount to & rate
        amountInputTo = new AmountInput(activity);
        amountInputTo.setOwner(activity);
        amountInputTo.setIncome();
        amountInputTo.disableIncomeExpenseButton();
        amountInputToNode = x.addEditNode(layout, R.string.amount_to, amountInputTo);
        amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
        amountInputFrom.setOnAmountChangedListener(onAmountFromChangedListener);
        setVisibility(amountInputToNode, View.GONE);
        rateInfoNode = addRateNode(layout);
        rate = (EditText)rateInfoNode.findViewById(R.id.rate);
        rate.addTextChangedListener(rateWatcher);
        rateInfo = (TextView)rateInfoNode.findViewById(R.id.data);
        bCalc = (ImageButton)rateInfoNode.findViewById(R.id.rateCalculator);
        bCalc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, CalculatorInput.class);
                intent.putExtra(AmountInput.EXTRA_AMOUNT, Utils.text(rate));
                activity.startActivityForResult(intent, EDIT_RATE);
            }
        });
        bDownload = (ImageButton)rateInfoNode.findViewById(R.id.rateDownload);
        bDownload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (selectedAccountFrom == null || selectedAccountTo == null) {
                    return;
                }
                new RateDownloadTask().execute();
            }
        });
        setVisibility(rateInfoNode, View.GONE);
    }

    private View addRateNode(LinearLayout layout) {
        return x.inflater.new Builder(layout, R.layout.select_entry_rate)
        .withLabel(R.string.rate)
        .withData(R.string.no_rate)
        .create();
    }

    public void selectFromAccount(Account account) {
        selectedAccountFrom = account;
        amountInputFrom.setCurrency(account.currency);
        checkNeedRate();
    }

    public void selectToAccount(Account account) {
        selectedAccountTo = account;
        amountInputTo.setCurrency(account.currency);
        checkNeedRate();
    }

    private void checkNeedRate() {
        if (isDifferentCurrencies()) {
            setVisibility(rateInfoNode, View.VISIBLE);
            setVisibility(amountInputToNode, View.VISIBLE);
            calculateRate();
        } else {
            setVisibility(rateInfoNode, View.GONE);
            setVisibility(amountInputToNode, View.GONE);
        }
    }

    private void calculateRate() {
        long amountFrom = amountInputFrom.getAmount();
        long amountTo = amountInputTo.getAmount();
        float r = 1.0f*amountTo/amountFrom;
        if (!Double.isNaN(r)) {
            setRate(r);
        }
        setRateInfo();
    }

    private void setRateInfo() {
        double r = getRate();
        StringBuilder sb = new StringBuilder();
        if (selectedAccountFrom != null && selectedAccountTo != null) {
            String currencyFrom = selectedAccountFrom.currency.name;
            String currencyTo = selectedAccountTo.currency.name;
            sb.append("1").append(currencyFrom).append("=").append(nf.format(r)).append(currencyTo).append(", ");
            sb.append("1").append(currencyTo).append("=").append(nf.format(1.0/r)).append(currencyFrom);
        }
        rateInfo.setText(sb.toString());
    }

    protected double getRate() {
        try {
            return Double.parseDouble(rate.getText().toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void setRate(double r) {
        rate.removeTextChangedListener(rateWatcher);
        rate.setText(nf.format(Math.abs(r)));
        rate.addTextChangedListener(rateWatcher);
    }

    public long getFromAmount() {
        return amountInputFrom.getAmount();
    }

    public long getToAmount() {
        if (isDifferentCurrencies()) {
            return amountInputTo.getAmount();
        } else {
            return -amountInputFrom.getAmount();
        }
    }

    private boolean isDifferentCurrencies() {
        return selectedAccountFrom != null && selectedAccountTo != null &&
               selectedAccountFrom.currency.id != selectedAccountTo.currency.id;
    }

    public void onActivityResult(int requestCode, Intent data) {
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
                updateToAmountFromRate();
            }
        }
    }

    private final AmountInput.OnAmountChangedListener onAmountFromChangedListener = new AmountInput.OnAmountChangedListener(){
        @Override
        public void onAmountChanged(long oldAmount, long newAmount) {
            double r = getRate();
            if (r > 0) {
                long amountFrom = amountInputFrom.getAmount();
                long amountTo = Math.round(r*amountFrom);
                amountInputTo.setOnAmountChangedListener(null);
                amountInputTo.setAmount(amountTo);
                amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
            } else {
                long amountFrom = amountInputFrom.getAmount();
                long amountTo = amountInputTo.getAmount();
                if (amountFrom > 0) {
                    setRate(1.0f * amountTo / amountFrom);
                }
            }
            setRateInfo();
            if (amountFromChangeListener != null) {
                amountFromChangeListener.onAmountChanged(oldAmount, newAmount);
            }
        }
    };

    private final AmountInput.OnAmountChangedListener onAmountToChangedListener = new AmountInput.OnAmountChangedListener(){
        @Override
        public void onAmountChanged(long oldAmount, long newAmount) {
            long amountFrom = amountInputFrom.getAmount();
            long amountTo = amountInputTo.getAmount();
            if (amountFrom > 0) {
                setRate(1.0d * amountTo / amountFrom);
            }
            setRateInfo();
            if (amountToChangeListener != null) {
                amountToChangeListener.onAmountChanged(oldAmount, newAmount);
            }
        }
    };

    public void setFromAmount(long fromAmount) {
        amountInputFrom.setAmount(fromAmount);
        calculateRate();
    }

    public void setToAmount(long toAmount) {
        amountInputTo.setAmount(toAmount);
        calculateRate();
    }

    private class RateDownloadTask extends AsyncTask<Void, Void, Double> {

        private final HttpClient httpClient = new DefaultHttpClient();
        private final Pattern pattern = Pattern.compile("<double.*?>(.+?)</double>");

        private String error;
        private ProgressDialog progressDialog;

        @Override
        protected Double doInBackground(Void... args) {
            HttpGet get = new HttpGet("http://www.webservicex.net/CurrencyConvertor.asmx/ConversionRate?FromCurrency="+ getFromCurrency() +"&ToCurrency="+ getToCurrency());
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
                    error = activity.getString(R.string.service_is_not_available);
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
            showProgressDialog();
            disableAll();
        }

        private void showProgressDialog() {
            String message = activity.getString(R.string.downloading_rate, getFromCurrency(), getToCurrency());
            progressDialog = ProgressDialog.show(activity, null, message, true, true, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel(true);
                }
            });
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            enableAll();
        }

        @Override
        protected void onPostExecute(Double result) {
            progressDialog.dismiss();
            enableAll();
            if (result == null) {
                if (error != null) {
                    Toast t = Toast.makeText(activity, error, Toast.LENGTH_LONG);
                    t.show();
                }
            } else {
                setRate(result);
                updateToAmountFromRate();
            }
        }

        private String getToCurrency() {
            return selectedAccountTo.currency.name;
        }

        private String getFromCurrency() {
            return selectedAccountFrom.currency.name;
        }

        private void disableAll() {
            amountInputFrom.setEnabled(false);
            amountInputTo.setEnabled(false);
            rate.setEnabled(false);
            bCalc.setEnabled(false);
            bDownload.setEnabled(false);
        }

        private void enableAll() {
            amountInputFrom.setEnabled(true);
            amountInputTo.setEnabled(true);
            rate.setEnabled(true);
            bCalc.setEnabled(true);
            bDownload.setEnabled(true);
        }

    }

    private final TextWatcher rateWatcher = new TextWatcher(){
        @Override
        public void afterTextChanged(Editable s) {
            updateToAmountFromRate();
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    private void updateToAmountFromRate() {
        double r = getRate();
        long amountFrom = amountInputFrom.getAmount();
        long amountTo = (long)Math.floor(r*amountFrom);
        amountInputTo.setOnAmountChangedListener(null);
        amountInputTo.setAmount(amountTo);
        setRateInfo();
        amountInputTo.setOnAmountChangedListener(onAmountToChangedListener);
    }

}
