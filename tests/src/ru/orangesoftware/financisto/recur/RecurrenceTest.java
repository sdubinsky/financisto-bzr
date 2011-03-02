package ru.orangesoftware.financisto.recur;

import android.test.AndroidTestCase;
import ru.orangesoftware.financisto.test.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.orangesoftware.financisto.test.DateTime.date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/18/11 7:15 PM
 */
public class RecurrenceTest extends AndroidTestCase {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void test_should_schedule_correctly_monthly_last_working_day() throws ParseException {
        assertDates(
                "2010-06-30T15:20:00~MONTHLY:count@1#interval@1#monthly_pattern_params_0@LAST-WEEKDAY#monthly_pattern_0@SPECIFIC_DAY#~INDEFINETELY:null",
                date(2011, 2, 18).atMidnight(),
                "2011-02-28 15:20:00,2011-03-31 15:20:00,2011-04-29 15:20:00,...");
    }

    public void test_should_schedule_correctly_on_the_same_day_if_the_schedule_time_is_after_the_current_time() throws Exception {
        assertDates(
                "2011-02-27T19:30:00~DAILY:interval@1#~INDEFINETELY:null",
                date(2011, 2, 27).at(12, 0, 0, 0),
                "2011-02-27 19:30:00,2011-02-28 19:30:00,...");
    }

    public void test_should_schedule_correctly_on_the_next_day_if_the_scheduled_time_is_before_the_current_time() throws Exception {
        assertDates(
                "2011-02-27T19:30:00~DAILY:interval@1#~INDEFINETELY:null",
                date(2011, 2, 27).at(20, 0, 0, 0),
                "2011-02-28 19:30:00,2011-03-01 19:30:00,...");
    }

    private void assertDates(String pattern, DateTime startDateTime, String datesAsString) throws ParseException {
        Recurrence r = Recurrence.parse(pattern);
        DateRecurrenceIterator ri = r.createIterator(startDateTime.asDate());
        assertTrue(ri.hasNext());
        String[] dates = datesAsString.split(",");
        for (String d : dates) {
            if ("...".equals(d)) {
                assertTrue(ri.hasNext());
                return;
            }
            assertEquals(d, formatDateTime(ri.next()));
        }
    }

    private String formatDateTime(Date d) throws ParseException {
        return sdf.format(d);
    }

}
