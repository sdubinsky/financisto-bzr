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
package ru.orangesoftware.financisto.model;

public class Total {
	
	public static final Total ZERO = new Total(Currency.EMPTY);
	
	public final Currency currency;
	public final boolean showAmount;
	public long amount;
	public long balance;
	
	public Total(Currency currency, boolean showAmount) {
		this.currency = currency;
		this.showAmount = showAmount;
	}

	public Total(Currency currency) {
		this.currency = currency;
		this.showAmount = false;
	}

}
