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

import ru.orangesoftware.financisto.R;

public enum CardIssuer {
	VISA(R.string.card_issuer_visa, R.drawable.account_type_card_visa), 
	VISA_ELECTRON(R.string.card_issuer_electron, R.drawable.account_type_card_visa_electron), 
	MASTERCARD(R.string.card_issuer_mastercard, R.drawable.account_type_card_mastercard), 
	MAESTRO(R.string.card_issuer_maestro, R.drawable.account_type_card_maestro), 
	AMEX(R.string.card_issuer_amex, R.drawable.account_type_card_amex),
	JCB(R.string.card_issuer_jcb, R.drawable.account_type_card_jcb),
	DINERS(R.string.card_issuer_diners, R.drawable.account_type_card_diners),
	
	DISCOVER(R.string.card_issuer_discover, R.drawable.account_type_card_discover);
	
	public final int titleId;
	public final int iconId;
	
	private CardIssuer(int titleId, int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}
	
}

