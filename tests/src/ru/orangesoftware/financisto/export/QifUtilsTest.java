/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.export;

import android.test.AndroidTestCase;
import ru.orangesoftware.financisto.test.DateTime;

import java.util.Arrays;

import static ru.orangesoftware.financisto.export.qif.QifUtils.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/12/11 12:27 AM
 */
public class QifUtilsTest extends AndroidTestCase {

    public void test_should_trim_first_char() {
        assertEquals("My Cash Account", trimFirstChar("NMy Cash Account"));
        assertEquals("-10.5", trimFirstChar("X-10.5"));
    }

    public void test_should_parse_dates() {
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("07/02/2011"));
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("07/02/2011", EU_FORMAT));
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("02/07/2011", US_FORMAT));
        assertEquals(DateTime.date(2011, 2, 7).atMidnight().asDate(), parseDate("07.02.11"));
    }

    public void test_should_parse_money() {
        assertEquals(100, parseMoney("1.0"));
        assertEquals(-100, parseMoney("-1."));
        assertEquals(10100, parseMoney("1,01"));
        assertEquals(100250, parseMoney("1,002.5"));
        assertEquals(100250, parseMoney("1.002,5"));
    }

    public void test_should_split_category_name() {
        assertEquals("P1", splitCategoryName("P1"));
        assertEquals("P1:c1", splitCategoryName("P1:c1"));
        assertEquals("P1", splitCategoryName("P1/C2"));
        assertEquals("P1:c1", splitCategoryName("P1:c1/C2"));
    }

}
