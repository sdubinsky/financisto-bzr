package ru.orangesoftware.financisto.model;

import android.test.AndroidTestCase;
import ru.orangesoftware.financisto.utils.Utils;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 3/16/11 9:23 PM
 */
public class CurrencyTest extends AndroidTestCase {

    public void test_should_format_amount_according_to_the_selected_currency() {
        Currency c = new Currency();
        c.decimals = 2;
        c.decimalSeparator = ".";
        c.groupSeparator = ",";
        c.symbol = "$";
        assertEquals("1,000.00$", Utils.amountToString(Currency.EMPTY, 100000));
    }

}
