package ru.orangesoftware.financisto.report;

import android.preference.PreferenceManager;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/6/11 11:15 PM
 */
public class PeriodReportTest extends AbstractReportTest {

    // important that report is re-created after include_transfers_into_reports preference is set

    public void test_should_calculate_correct_report_for_today_without_transfers() {
        assertTrue(PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("include_transfers_into_reports", false).commit());
        report = createReport();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        List<GraphUnit> units = assertReportReturnsData();
        assertIncome(units.get(0), a1.currency, 0);
        assertExpense(units.get(0), a1.currency, -3500);
        assertIncome(units.get(0), a2.currency, 0);
    }

    public void test_should_calculate_correct_report_for_today_with_transfers() {
        assertTrue(PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("include_transfers_into_reports", true).commit());
        report = createReport();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        List<GraphUnit> units = assertReportReturnsData();
        assertIncome(units.get(0), a1.currency, 0);
        assertExpense(units.get(0), a1.currency, -4700);
        assertIncome(units.get(0), a2.currency, 250);
    }

    @Override
    protected Report createReport() {
        return new PeriodReport(getContext());
    }

}
