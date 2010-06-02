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


/*default*/ class CurrencyAmount {	
	
	public final String currency;
	public final int amount;
	public final float percentage;
	
	String amountText;
	int amountTextWidth;
	int amountTextHeight;
	int lineWidth;
		
	public CurrencyAmount(String currency, int amount, float percentage) {
		this.currency = currency;
		this.amount = amount;
		this.percentage = percentage;
	}
}
