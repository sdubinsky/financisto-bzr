/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/
package ru.orangesoftware.financisto.model;

import android.content.Intent;

import javax.persistence.*;

@Entity
@Table(name = "split")
public class Split {

    private static final String SPLIT_ID = "SPLIT_ID";
    private static final String SPLIT_ACCOUNT_ID = "SPLIT_ACCOUNT_ID";
    private static final String SPLIT_CATEGORY_ID = "SPLIT_CATEGORY_ID";
    private static final String SPLIT_AMOUNT = "SPLIT_AMOUNT";
    private static final String SPLIT_NOTE = "SPLIT_NOTE";
    private static final String SPLIT_UNSPLIT_AMOUNT = "SPLIT_UNSPLIT_AMOUNT";

    @Id
    @Column(name = "_id")
    public long id = -1;

	@Column(name = "transaction_id")
	public long transactionId;

    @Column(name = "category_id")
    public long categoryId;

	@Column(name = "amount")
	public long amount;

    @Column(name = "note")
    public String note;

    @Transient
    public long accountId;

    @Transient
    public long unsplitAmount;

    public static Split fromIntent(Intent intent) {
        Split split = new Split();
        if (intent != null) {
            split.id = intent.getLongExtra(SPLIT_ID, -1);
            split.accountId = intent.getLongExtra(SPLIT_ACCOUNT_ID, -1);
            split.categoryId = intent.getLongExtra(SPLIT_CATEGORY_ID, 0);
            split.amount = intent.getLongExtra(SPLIT_AMOUNT, 0);
            split.unsplitAmount = intent.getLongExtra(SPLIT_UNSPLIT_AMOUNT, 0);
            split.note = intent.getStringExtra(SPLIT_NOTE);
        }
        return split;
    }

    public void toIntent(Intent intent) {
        intent.putExtra(SPLIT_ID, id);
        intent.putExtra(SPLIT_ACCOUNT_ID, accountId);
        intent.putExtra(SPLIT_CATEGORY_ID, categoryId);
        intent.putExtra(SPLIT_AMOUNT, amount);
        intent.putExtra(SPLIT_UNSPLIT_AMOUNT, unsplitAmount);
        intent.putExtra(SPLIT_NOTE, note);
    }

}
