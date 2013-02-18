/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import android.test.InstrumentationTestCase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import ru.orangesoftware.financisto.http.FakeHttpClientWrapper;
import ru.orangesoftware.financisto.model.Currency;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/18/13
 * Time: 10:33 PM
 */
public abstract class AbstractRatesDownloaderTest extends InstrumentationTestCase {

    FakeHttpClientWrapper client = new FakeHttpClientWrapper();

    abstract ExchangeRateProvider service();

    ExchangeRate downloadRate(String from, String to) {
        return service().getRate(currency(from), currency(to));
    }

    private Currency currency(String name) {
        Currency c = new Currency();
        c.name = name;
        return c;
    }

    void givenResponseFromWebService(String url, String response) {
        client.givenResponse(url, response);
    }

    void givenExceptionWhileRequestingWebService() {
        client.error = new ConnectTimeoutException("Timeout");
    }

    static String anyUrl() {
        return "*";
    }

}
