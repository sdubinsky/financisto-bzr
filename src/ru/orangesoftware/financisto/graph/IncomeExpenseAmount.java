package ru.orangesoftware.financisto.graph;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/7/11 12:56 AM
 */
public class IncomeExpenseAmount {

    public float income;
    public float expense;

    public void add(float amount, boolean forceIncome) {
        if (forceIncome || amount > 0) {
            income += amount;
        } else {
            expense += amount;
        }
    }

    public long max() {
        return (long)Math.max(Math.abs(income), Math.abs(expense));
    }

}
