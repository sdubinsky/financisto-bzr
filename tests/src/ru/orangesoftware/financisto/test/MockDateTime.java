package ru.orangesoftware.financisto.test;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 8:32 PM
 */
public class MockDateTime {

    private final Calendar c = Calendar.getInstance();

    private MockDateTime() {}

    public static MockDateTime on() {
        return new MockDateTime();
    }

    public static MockDateTime date(int year, int month, int day) {
        MockDateTime dt = new MockDateTime();
        dt.c.set(Calendar.YEAR, year);
        dt.c.set(Calendar.MONTH, month-1);
        dt.c.set(Calendar.DAY_OF_MONTH, day);
        return dt;
    }

    public MockDateTime atMidnight() {
        return at(0, 0, 0, 0);
    }

    public MockDateTime atNoon() {
        return at(12, 0, 0, 0);
    }

    public MockDateTime atDayEnd() {
        return at(23, 59, 59, 999);
    }

    public MockDateTime at(int hh, int mm, int ss, int ms) {
        c.set(Calendar.HOUR_OF_DAY, hh);
        c.set(Calendar.MINUTE, mm);
        c.set(Calendar.SECOND, ss);
        c.set(Calendar.MILLISECOND, ms);
        return this;
    }

    public long asLong() {
        return c.getTimeInMillis();
    }

    public Date asDate() {
        return c.getTime();
    }

}
