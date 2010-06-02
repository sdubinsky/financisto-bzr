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
import ru.orangesoftware.financisto.utils.LocalizableEnum;

public enum TransactionStatus implements LocalizableEnum {
	PN(R.string.transaction_status_pending, R.drawable.transaction_status_pending_2),
	UR(R.string.transaction_status_unreconciled, R.drawable.transaction_status_unreconciled_2),
	CL(R.string.transaction_status_cleared, R.drawable.transaction_status_cleared_2),
	RC(R.string.transaction_status_reconciled, R.drawable.transaction_status_reconciled_2);
	
	public final int titleId;
	public final int iconId;
	
	private TransactionStatus(int titleId, int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@Override
	public int getTitleId() {
		return titleId;
	}

}
