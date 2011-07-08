/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.graph;

import java.util.*;

import ru.orangesoftware.financisto.model.Currency;

public class GraphUnit implements Comparable<GraphUnit>, Iterable<Amount> {

	public final long id;
	public final String name;
	public final GraphStyle style;

    private final Map<Currency, IncomeExpenseAmount> currencyToAmountMap = new LinkedHashMap<Currency, IncomeExpenseAmount>(2);
    private final List<Amount> amounts = new LinkedList<Amount>();

    private long maxAmount;
	
	public GraphUnit(long id, String name, GraphStyle style) {
		this.id = id;
		this.name = name != null ? name : "";
		this.style = style;
	}
	
	public void addAmount(Currency currency, long amount) {
        if (amount == 0) {
            return;
        }
        IncomeExpenseAmount incomeExpenseAmount = getIncomeExpense(currency);
        incomeExpenseAmount.add(amount);
	}

    public IncomeExpenseAmount getIncomeExpense(Currency currency) {
        IncomeExpenseAmount amount = currencyToAmountMap.get(currency);
        if (amount == null) {
            amount = new IncomeExpenseAmount();
            currencyToAmountMap.put(currency, amount);
        }
        return amount;
    }

    public void flatten() {
        if (amounts.isEmpty()) {
            long maxAmount = 0;
            for (Map.Entry<Currency, IncomeExpenseAmount> entry : currencyToAmountMap.entrySet()) {
                Currency currency = entry.getKey();
                IncomeExpenseAmount amount = entry.getValue();
                addToAmounts(currency, amount.income);
                addToAmounts(currency, amount.expense);
                maxAmount = Math.max(maxAmount, Math.max(Math.abs(amount.income), Math.abs(amount.expense)));
            }
            Collections.sort(amounts);
            this.maxAmount = maxAmount;
        }
    }

    private void addToAmounts(Currency currency, long amount) {
        if (amount != 0) {
            amounts.add(new Amount(currency, amount));
        }
    }

    @Override
	public int compareTo(GraphUnit that) {
		return that.maxAmount == this.maxAmount ? 0 : (that.maxAmount > this.maxAmount ? 1 : -1);
	}

    @Override
    public Iterator<Amount> iterator() {
        return amounts.iterator();
    }

    public int size() {
        return amounts.size();
    }

}
