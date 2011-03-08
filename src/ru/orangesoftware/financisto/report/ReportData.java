package ru.orangesoftware.financisto.report;

import ru.orangesoftware.financisto.graph.GraphUnit;
import ru.orangesoftware.financisto.model.Total;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/28/11 9:16 PM
 */
public class ReportData {

    public final ArrayList<GraphUnit> units;
    public final Total[] totals;

    public ReportData(ArrayList<GraphUnit> units, Total[] totals) {
        this.units = units;
        this.totals = totals;
    }

}
