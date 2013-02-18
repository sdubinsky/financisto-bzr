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
 * Date: 2/18/13
 * Time: 10:04 PM
 */
public class WebserviceXExchangeRatesDownloaderTest extends AbstractRatesDownloaderTest {

    WebserviceXExchangeRatesDownloader webserviceX = new WebserviceXExchangeRatesDownloader(client);

    @Override
    ExchangeRateProvider service() {
        return webserviceX;
    }

    public void test_should_download_single_rate_cur_to_cur() {
        //given
        givenResponseFromWebService("http://www.webservicex.net/CurrencyConvertor.asmx/ConversionRate?FromCurrency=USD&ToCurrency=SGD",
                "<double xmlns=\"http://www.webserviceX.NET/\">1.2387</double>");
        //when
        ExchangeRate exchangeRate = downloadRate("USD", "SGD");
        //then
        assertEquals(1.2387, exchangeRate.rate);
    }

    public void test_should_skip_unknown_currency() {
        //given
        givenResponseFromWebService(anyUrl(), "Exception: Unable to convert ToCurrency to Currency\r\nStacktrace...");
        //then
        assertFalse(downloadRate("USD", "AAA").isOk());
    }

    public void test_should_handle_error_from_webservice_properly() {
        //given
        givenResponseFromWebService(anyUrl(), "System.IO.IOException: There is not enough space on the disk.\r\nStacktrace...");
        //when
        ExchangeRate downloadedRate = downloadRate("USD", "SGD");
        //then
        assertFalse(downloadedRate.isOk());
        assertEquals("Something wrong with the exchange rates provider. Response from the service - System.IO.IOException: There is not enough space on the disk.",
                downloadedRate.getErrorMessage());
    }

    public void test_should_handle_runtime_error_properly() {
        //given
        givenExceptionWhileRequestingWebService();
        //when
        ExchangeRate downloadedRate = downloadRate("USD", "SGD");
        //then
        assertFalse(downloadedRate.isOk());
        assertEquals("Unable to get exchange rates: Timeout", downloadedRate.getErrorMessage());
    }

}
