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

	public long id;
	public String name;
	public final HashMap<String, Amount> amounts = new HashMap<String, Amount>();
	long sum = 0;
	
	public GraphUnit(long id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public void addAmount(Currency currency, long amount) {
		String key = currency.symbol+(amount >= 0 ? "_1" : "_2");
		Amount a = amounts.get(key);
		if (a == null) {
			a = new Amount(currency, amount);
			amounts.put(key, a);
		} else {
			a.add(amount);
		}
		sum += Math.abs(amount);
	}
	
	@Override
	public int compareTo(GraphUnit another) {
		return another.sum == this.sum ? 0 : (another.sum > this.sum ? 1 : -1);
	}
	
}
