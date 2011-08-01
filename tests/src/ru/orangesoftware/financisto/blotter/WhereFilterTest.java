/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.blotter;

import android.test.AndroidTestCase;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/1/11 10:35 PM
 */
public class WhereFilterTest extends AndroidTestCase {

    public void test_filter_should_support_raw_criteria() {
        WhereFilter filter = WhereFilter.empty();
        filter.put(WhereFilter.Criteria.eq("from_account_id", "1"));
        filter.put(WhereFilter.Criteria.raw("parent_id=0 OR is_transfer=-1"));
        assertEquals("from_account_id =? AND (parent_id=0 OR is_transfer=-1)", filter.getSelection());
        assertEquals(new String[]{"1"}, filter.getSelectionArgs());
    }

    public static void assertEquals(String[] expected, String[] actual) {
        assertEquals(Arrays.toString(expected), Arrays.toString(actual));
    }

}
