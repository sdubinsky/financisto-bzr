package ru.orangesoftware.financisto.test;

import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 8:32 PM
 */
public class MockDate {

    private final Calendar c = Calendar.getInstance();

    private MockDate() {}

    public static MockDate thisYear() {
        return new MockDate();
    }

    public MockDate jan() {
        c.set(Calendar.MONTH, Calendar.JANUARY);
        return this;
    }

    public MockDate feb() {
        c.set(Calendar.MONTH, Calendar.FEBRUARY);
        return this;
    }

    public MockDate day(int day) {
        c.set(Calendar.DAY_OF_MONTH, day);
        return this;
    }

    public MockDate atMidnight() {
        return at(0, 0, 0, 0);
    }

    public MockDate atNoon() {
        return at(12, 0, 0, 0);
    }

    public MockDate atDayEnd() {
        return at(23, 59, 59, 999);
    }

    public MockDate at(int hh, int mm, int ss, int ms) {
        c.set(Calendar.HOUR_OF_DAY, hh);
        c.set(Calendar.MINUTE, mm);
        c.set(Calendar.SECOND, ss);
        c.set(Calendar.MILLISECOND, ms);
        return this;
    }

    public long asLong() {
        return c.getTimeInMillis();
    }

}
