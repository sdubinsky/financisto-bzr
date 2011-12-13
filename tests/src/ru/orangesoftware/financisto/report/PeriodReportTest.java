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
        //given
        givenTransfersAreExcludedFromReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), currency, 0);
        assertExpense(units.get(0), currency, -3500);
    }

    public void test_should_calculate_correct_report_for_today_with_transfers() {
        //given
        givenTransfersAreIncludedIntoReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-1000).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-2500).create();
        TransferBuilder.withDb(db).fromAccount(a1).toAccount(a2).fromAmount(-1200).toAmount(250).dateTime(DateTime.today()).create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        assertIncome(units.get(0), currency, 250);
        assertExpense(units.get(0), currency, -4700);
    }

    public void test_should_calculate_correct_report_for_today_with_splits_without_transfers() {
        //given
        givenTransfersAreExcludedFromReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-10000)
                .withSplit(categories.get("A1"), -8000)
                .withTransferSplit(a2, -2000, 2000)
                .create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), currency, 0);
        assertExpense(units.get(0), currency, -8000);
    }

    public void test_should_calculate_correct_report_for_today_with_splits_with_transfers() {
        //given
        givenTransfersAreIncludedIntoReports();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-10000)
                .withSplit(categories.get("A1"), -8000)
                .withTransferSplit(a2, -2000, 2000)
                .create();
        //when
        report = createReport();
        List<GraphUnit> units = assertReportReturnsData();
        //then
        assertIncome(units.get(0), currency, 2000);
        assertExpense(units.get(0), currency, -10000);
    }

    private void givenTransfersAreExcludedFromReports() {
        assertTrue(PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("include_transfers_into_reports", false).commit());
    }

    private void givenTransfersAreIncludedIntoReports() {
        assertTrue(PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("include_transfers_into_reports", true).commit());
    }

    @Override
    protected Report createReport() {
        return new PeriodReport(getContext());
    }

}
