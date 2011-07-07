package ru.orangesoftware.financisto.report;

import ru.orangesoftware.financisto.blotter.WhereFilter;
import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/6/11 11:15 PM
 */
public class SubCategoryReportTest extends AbstractReportTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CategoryReportTopDown r = new CategoryReportTopDown(getContext());
        filter = r.createFilterForSubCategory(db, WhereFilter.empty(), categories.get("A").id);
    }

    public void test_should_calculate_correct_report() {
        // A -3400
        //   +250
        // A2 -2200
        //    +250
        // A1 -1100
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A")).dateTime(DateTime.today()).amount(-100).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A1")).dateTime(DateTime.today()).amount(-100).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("A2")).dateTime(DateTime.today()).amount(250).create();
        TransactionBuilder.withDb(db).account(a1).category(categories.get("B")).dateTime(DateTime.today()).amount(500).create();
        TransactionBuilder.withDb(db).account(a1).dateTime(DateTime.today()).amount(-5000)
                .withSplit(categories.get("A1"), -1000)
                .withSplit(categories.get("A2"), -2200)
                .withSplit(categories.get("B"), -1800)
                .create();
        List<GraphUnit> units = assertReportReturnsData();
        assertName(units.get(0), "A");
        assertExpense(units.get(0), -3400);
        assertIncome(units.get(0), 250);
        assertName(units.get(1), "A2");
        assertExpense(units.get(1), -2200);
        assertIncome(units.get(1), 250);
        assertName(units.get(2), "A1");
        assertExpense(units.get(2), -1100);
    }

    @Override
    protected Report createReport() {
        return new SubCategoryReport(getContext());
    }

}
