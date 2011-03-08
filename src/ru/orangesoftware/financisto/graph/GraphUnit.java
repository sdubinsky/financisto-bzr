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

import java.util.HashMap;

import ru.orangesoftware.financisto.model.Currency;

public class GraphUnit implements Comparable<GraphUnit> {

	public final long id;
	public final String name;
	public final GraphStyle style;
	public final HashMap<String, Amount> amounts = new HashMap<String, Amount>(2);

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
		String key = currency.symbol+(amount >= 0 ? "_1" : "_2");
		Amount a = amounts.get(key);
		if (a == null) {
			a = new Amount(currency, amount);
			amounts.put(key, a);
		} else {
			a.add(amount);
		}
	}

    public void calculateMaxAmount() {
        long maxAmount = 0;
        for (Amount a : amounts.values()) {
            maxAmount = Math.max(maxAmount, Math.abs(a.amount));
        }
        this.maxAmount = maxAmount;
    }

	@Override
	public int compareTo(GraphUnit another) {
		return another.maxAmount > this.maxAmount ? 1 : -1;
	}

}
