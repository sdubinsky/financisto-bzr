/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.datetime;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.LocalizableEnum;

import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 12/17/12 9:08 PM
 */
public enum PeriodType implements LocalizableEnum {
    TODAY(R.string.period_today) {
        @Override
        public Period calculatePeriod(long refTime) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(refTime);
            long start = DateUtils.startOfDay(c).getTimeInMillis();
            long end = DateUtils.endOfDay(c).getTimeInMillis();
            return new Period(PeriodType.TODAY, start, end);
        }
    },
    YESTERDAY(R.string.period_yesterday) {
        @Override
        public Period calculatePeriod(long refTime) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(refTime);
            c.add(Calendar.DAY_OF_MONTH, -1);
            long start = DateUtils.startOfDay(c).getTimeInMillis();
            long end = DateUtils.endOfDay(c).getTimeInMillis();
            return new Period(PeriodType.YESTERDAY, start, end);
        }
    },
    THIS_WEEK(R.string.period_this_week) {
        @Override
        public Period calculatePeriod(long refTime) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(refTime);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            long start, end;
            if (dayOfWeek == Calendar.MONDAY) {
                start = DateUtils.startOfDay(c).getTimeInMillis();
                c.add(Calendar.DAY_OF_MONTH, 6);
                end = DateUtils.endOfDay(c).getTimeInMillis();
            } else {
                c.add(Calendar.DAY_OF_MONTH, -(dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - 2));
                start = DateUtils.startOfDay(c).getTimeInMillis();
                c.add(Calendar.DAY_OF_MONTH, 6);
                end = DateUtils.endOfDay(c).getTimeInMillis();
            }
            return new Period(PeriodType.THIS_WEEK, start, end);
        }
    },
    THIS_MONTH(R.string.period_this_month) {
        @Override
        public Period calculatePeriod(long refTime) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(refTime);
            c.set(Calendar.DAY_OF_MONTH, 1);
            long start = DateUtils.startOfDay(c).getTimeInMillis();
            c.add(Calendar.MONTH, 1);
            c.add(Calendar.DAY_OF_MONTH, -1);
            long end = DateUtils.endOfDay(c).getTimeInMillis();
            return new Period(PeriodType.THIS_MONTH, start, end);
        }
    },
    LAST_WEEK(R.string.period_last_week) {
        @Override
        public Period calculatePeriod(long refTime) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(refTime);
            c.add(Calendar.DAY_OF_YEAR, -7);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            long start, end;
            if (dayOfWeek == Calendar.MONDAY) {
                start = DateUtils.startOfDay(c).getTimeInMillis();
                c.add(Calendar.DAY_OF_MONTH, 6);
                end = DateUtils.endOfDay(c).getTimeInMillis();
            } else {
                c.add(Calendar.DAY_OF_MONTH, -(dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - 2));
                start = DateUtils.startOfDay(c).getTimeInMillis();
                c.add(Calendar.DAY_OF_MONTH, 6);
                end = DateUtils.endOfDay(c).getTimeInMillis();
            }
            return new Period(PeriodType.LAST_WEEK, start, end);
        }
    },
    LAST_MONTH(R.string.period_last_month) {
        @Override
        public Period calculatePeriod(long refTime) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(refTime);
            c.add(Calendar.MONTH, -1);
            c.set(Calendar.DAY_OF_MONTH, 1);
            long start = DateUtils.startOfDay(c).getTimeInMillis();
            c.add(Calendar.MONTH, 1);
            c.add(Calendar.DAY_OF_MONTH, -1);
            long end = DateUtils.endOfDay(c).getTimeInMillis();
            return new Period(PeriodType.LAST_MONTH, start, end);
        }
    },
    THIS_AND_LAST_WEEK(R.string.period_this_and_last_week) {
        @Override
        public Period calculatePeriod(long refTime) {
            Period lastWeek = LAST_WEEK.calculatePeriod(refTime);
            Period thisWeek = THIS_WEEK.calculatePeriod(refTime);
            return new Period(PeriodType.THIS_AND_LAST_WEEK, lastWeek.start, thisWeek.end);
        }
    },
    THIS_AND_LAST_MONTH(R.string.period_this_and_last_month) {
        @Override
        public Period calculatePeriod(long refTime) {
            Period lastMonth = LAST_MONTH.calculatePeriod(refTime);
            Period thisMonth = THIS_MONTH.calculatePeriod(refTime);
            return new Period(PeriodType.THIS_AND_LAST_MONTH, lastMonth.start, thisMonth.end);
        }
    },
    CUSTOM(R.string.period_custom) {
        @Override
        public Period calculatePeriod(long refTime) {
            return null;
        }
    };

    public final int titleId;

    PeriodType(int titleId) {
        this.titleId = titleId;
    }

    public int getTitleId() {
        return titleId;
    }

    public abstract Period calculatePeriod(long refTime);

    public Period calculatePeriod() {
        return calculatePeriod(System.currentTimeMillis());
    }
}
