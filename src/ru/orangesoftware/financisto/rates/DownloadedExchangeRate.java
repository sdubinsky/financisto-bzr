/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/16/13
 * Time: 6:26 PM
 */
public class DownloadedExchangeRate {

    public final ExchangeRate exchangeRate;
    public final String error;

    public DownloadedExchangeRate(ExchangeRate exchangeRate) {
        this(exchangeRate, null);
    }

    public DownloadedExchangeRate(ExchangeRate exchangeRate, String error) {
        this.exchangeRate = exchangeRate;
        this.error = error;
    }

    public boolean isOk() {
        return error == null;
    }

    public String getErrorMessage() {
        return error != null ? error : "";
    }
}
