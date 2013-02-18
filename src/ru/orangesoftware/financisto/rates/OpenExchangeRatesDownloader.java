/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import android.text.TextUtils;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import ru.orangesoftware.financisto.http.HttpClientWrapper;
import ru.orangesoftware.financisto.model.Currency;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/16/13
 * Time: 6:27 PM
 */
//@NotThreadSafe
public class OpenExchangeRatesDownloader implements ExchangeRateProvider {

    private static final String TAG = OpenExchangeRatesDownloader.class.getSimpleName();
    private static final String GET_LATEST = "http://openexchangerates.org/api/latest.json?app_id=";

    private final String appId;
    private final HttpClientWrapper httpClient;

    private JSONObject json;

    public OpenExchangeRatesDownloader(HttpClientWrapper httpClient, String appId) {
        this.httpClient = httpClient;
        this.appId = appId;
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency) {
        try {
            downloadLatestRates();
            if (hasError(json)) {
                return error(json);
            }
            return getRate(json, fromCurrency, toCurrency);
        } catch (Exception e) {
            return error(e);
        }
    }

    private void downloadLatestRates() throws Exception {
        if (json == null) {
            if (appIdIsNotSet()) {
                throw new RuntimeException("App ID is not set");
            }
            Log.i(TAG, "Downloading latest rates...");
            json = httpClient.getAsJson(getLatestUrl());
            Log.i(TAG, json.toString());
        }
    }

    private boolean appIdIsNotSet() {
        return TextUtils.getTrimmedLength(appId) == 0;
    }

    private String getLatestUrl() {
        return GET_LATEST+appId;
    }

    private boolean hasError(JSONObject json) throws JSONException {
        return json.optBoolean("error", false);
    }

    private ExchangeRate error(JSONObject json) {
        String status = json.optString("status");
        String message = json.optString("message");
        String description = json.optString("description");
        return ExchangeRate.error(status+" ("+message+"): "+description);
    }

    private ExchangeRate error(Exception e) {
        return ExchangeRate.error("Unable to get exchange rates: "+e.getMessage());
    }

    private ExchangeRate getRate(JSONObject json, Currency fromCurrency, Currency toCurrency) throws JSONException {
        if (json.has("rates")) {
            JSONObject rates = json.getJSONObject("rates");
            if (rates.has(fromCurrency.name) && rates.has(toCurrency.name)) {
                double usdFrom = rates.getDouble(fromCurrency.name);
                double usdTo = rates.getDouble(toCurrency.name);
                double rate = usdTo * (1 / usdFrom);
                ExchangeRate exchangeRate = rate(fromCurrency, toCurrency, rate);
                updateDateTime(json, exchangeRate);
                return exchangeRate;
            }
        }
        return ExchangeRate.NA;
    }

    private ExchangeRate rate(Currency fromCurrency, Currency toCurrency, double rate) {
        ExchangeRate r = new ExchangeRate();
        r.fromCurrencyId = fromCurrency.id;
        r.toCurrencyId = toCurrency.id;
        r.rate = rate;
        return r;
    }

    private void updateDateTime(JSONObject json, ExchangeRate rate) throws JSONException {
        rate.date = 1000*json.optLong("timestamp", System.currentTimeMillis());
    }

    @Override
    public ExchangeRate getRate(Currency fromCurrency, Currency toCurrency, long atTime) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
