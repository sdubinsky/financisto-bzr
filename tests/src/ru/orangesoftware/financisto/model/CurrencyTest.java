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
        c.decimals = 1;
        c.decimalSeparator = "','";
        c.groupSeparator = "''";
        c.symbol = "$";
        String actualString = Utils.amountToString(c, 100000);
        assertEquals("1000,0 $", actualString);
    }

    public void test_should_format_symbol_according_to_the_selected_format() {
        //given
        Currency c = new Currency();
        c.decimals = 1;
        c.decimalSeparator = "'.'";
        c.groupSeparator = "','";
        c.symbol = "$";
        //when
        c.symbolFormat = SymbolFormat.RS;
        assertEquals("1,000.0 $", Utils.amountToString(c, 100000));
        assertEquals("+1,000.0 $", Utils.amountToString(c, 100000, true));
        assertEquals("-1,000.0 $", Utils.amountToString(c, -100000));
        //when
        c.symbolFormat = SymbolFormat.R;
        assertEquals("1,000.0$", Utils.amountToString(c, 100000));
        assertEquals("+1,000.0$", Utils.amountToString(c, 100000, true));
        assertEquals("-1,000.0$", Utils.amountToString(c, -100000));
        //when
        c.symbolFormat = SymbolFormat.LS;
        assertEquals("$ 1,000.0", Utils.amountToString(c, 100000));
        assertEquals("+$ 1,000.0", Utils.amountToString(c, 100000, true));
        assertEquals("-$ 1,000.0", Utils.amountToString(c, -100000));
        //when
        c.symbolFormat = SymbolFormat.L;
        assertEquals("$1,000.0", Utils.amountToString(c, 100000));
        assertEquals("+$1,000.0", Utils.amountToString(c, 100000, true));
        assertEquals("-$1,000.0", Utils.amountToString(c, -100000));
    }

}
