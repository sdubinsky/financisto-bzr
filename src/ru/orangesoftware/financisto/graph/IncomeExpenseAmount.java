package ru.orangesoftware.financisto.graph;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/7/11 12:56 AM
 */
public class IncomeExpenseAmount {

    public long income;
    public long expense;

    public void add(long amount, boolean forceIncome) {
        if (forceIncome || amount > 0) {
            income += amount;
        } else {
            expense += amount;
        }
    }

}
