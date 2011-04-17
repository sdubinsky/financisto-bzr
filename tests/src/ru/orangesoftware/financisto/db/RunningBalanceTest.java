/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.db;

import android.database.Cursor;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.test.AccountBuilder;
import ru.orangesoftware.financisto.test.DateTime;
import ru.orangesoftware.financisto.test.TransactionBuilder;
import ru.orangesoftware.financisto.test.TransferBuilder;

public class RunningBalanceTest extends AbstractDbTest {

    public void test_should_update_running_balance_for_single_account() {
        Account a = AccountBuilder.createDefault(db);
        Transaction t1 = TransactionBuilder.withDb(db).account(a).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a).amount(1234).create();
        db.rebuildRunningBalanceForAccount(a);
        assertAccountBalanceForTransaction(t1, a, 1000);
        assertAccountBalanceForTransaction(t2, a, 2234);
        assertFinalBalanceForAccount(a, 2234);
    }

    public void test_should_update_running_balance_for_two_accounts() {
        Account a1 = AccountBuilder.createDefault(db);
        Account a2 = AccountBuilder.createDefault(db);
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a2).amount(2000).create();
        Transaction t3 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-500).toAccount(a2).toAmount(500).create();
        db.rebuildRunningBalance();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a2, 2000);
        assertAccountBalanceForTransaction(t3, a1, 500);
        assertAccountBalanceForTransaction(t3, a2, 2500);
        assertFinalBalanceForAccount(a1, 500);
        assertFinalBalanceForAccount(a2, 2500);
    }

    public void test_should_update_running_balance_when_inserting_new_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11,5,0,0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12,0,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        // t4 | 13:20 | +100   | +350 <- insert at the bottom
        Transaction t4 = TransactionBuilder.withDb(db).account(a1).amount(100).dateTime(DateTime.today().at(13,20,0,0)).create();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertAccountBalanceForTransaction(t4, a1, 350);
        assertFinalBalanceForAccount(a1, 350);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t5 | 11:10 | -50    | +450 <- insert in the middle
        // t3 | 12:00 | -250   | +200
        // t4 | 13:20 | +100   | +300
        Transaction t5 = TransactionBuilder.withDb(db).account(a1).amount(-50).dateTime(DateTime.today().at(11,10,0,0)).create();
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t5, a1, 450);
        assertAccountBalanceForTransaction(t3, a1, 200);
        assertAccountBalanceForTransaction(t4, a1, 300);
        assertFinalBalanceForAccount(a1, 300);
        // *  | time  | amount | balance
        // t6 | 10:00 | +150   | +150 <- insert at the top
        // t1 | 11:00 | +1000  | +1150
        // t2 | 11:05 | -500   | +650
        // t5 | 11:10 | -50    | +600
        // t3 | 12:00 | -250   | +350
        // t4 | 13:20 | +100   | +450
        Transaction t6 = TransactionBuilder.withDb(db).account(a1).amount(150).dateTime(DateTime.today().at(10,0,0,0)).create();
        assertAccountBalanceForTransaction(t6, a1, 150);
        assertAccountBalanceForTransaction(t1, a1, 1150);
        assertAccountBalanceForTransaction(t2, a1, 650);
        assertAccountBalanceForTransaction(t5, a1, 600);
        assertAccountBalanceForTransaction(t3, a1, 350);
        assertAccountBalanceForTransaction(t4, a1, 450);
        assertFinalBalanceForAccount(a1, 450);
    }


    public void test_should_update_running_balance_when_updating_amount_on_existing_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11,5,0,0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12,0,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -350   | +150 <- update at the bottom
        t3.fromAmount = -350;
        db.insertOrUpdate(t3);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -400   | +600 <- update in the middle
        // t3 | 12:00 | -350   | +250
        t2.fromAmount = -400;
        db.insertOrUpdate(t2);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 600);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1200  | +1200 <- update at the top
        // t2 | 11:05 | -400   | +800
        // t3 | 12:00 | -350   | +450
        t1.fromAmount = 1200;
        db.insertOrUpdate(t1);
        assertAccountBalanceForTransaction(t1, a1, 1200);
        assertAccountBalanceForTransaction(t2, a1, 800);
        assertAccountBalanceForTransaction(t3, a1, 450);
        assertFinalBalanceForAccount(a1, 450);
    }

    public void test_should_update_running_balance_when_updating_datetime_on_existing_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11,5,0,0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12,0,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t3 | 11:01 | -250   | +750 <- move up
        // t2 | 11:05 | -500   | +250
        t3.dateTime = DateTime.today().at(11,1,0,0).asLong();
        db.insertOrUpdate(t3);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t3, a1, 750);
        assertAccountBalanceForTransaction(t2, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:05 | -250   | +250 <- move down
        t3.dateTime = DateTime.today().at(12,5,0,0).asLong();
        db.insertOrUpdate(t3);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
    }

    public void test_should_update_running_balance_when_deleting_existing_transaction() {
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        // t4 | 13:00 | -50    | +200
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t1 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t2 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11,5,0,0)).create();
        Transaction t3 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12,0,0,0)).create();
        Transaction t4 = TransactionBuilder.withDb(db).account(a1).amount(-50).dateTime(DateTime.today().at(13,0,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertAccountBalanceForTransaction(t4, a1, 200);
        assertFinalBalanceForAccount(a1, 200);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | -500   | +500
        // t3 | 12:00 | -250   | +250
        // t4 | 13:00 | *      | * <- delete at the bottom
        db.deleteTransaction(t4.id);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t2, a1, 500);
        assertAccountBalanceForTransaction(t3, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        // *  | time  | amount | balance
        // t1 | 11:00 | +1000  | +1000
        // t2 | 11:05 | *      | * <- delete in the middle
        // t3 | 12:00 | -250   | +750
        db.deleteTransaction(t2.id);
        assertAccountBalanceForTransaction(t1, a1, 1000);
        assertAccountBalanceForTransaction(t3, a1, 750);
        assertFinalBalanceForAccount(a1, 750);
        // *  | time  | amount | balance
        // t1 | 11:00 | *      | * <- delete at the top
        // t3 | 12:00 | -250   | -250
        db.deleteTransaction(t1.id);
        assertAccountBalanceForTransaction(t3, a1, -250);
        assertFinalBalanceForAccount(a1, -250);
    }

    public void test_should_update_running_balance_when_inserting_new_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t22 | 12:00 | -100   | +800
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t12 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11,5,0,0)).create();
        Transaction t13 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12,0,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertFinalBalanceForAccount(a1, 250);
        Account a2 = AccountBuilder.createDefault(db);
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t22 = TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13,0,0,0)).create();
        db.rebuildRunningBalanceForAccount(a2);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t22, a2, 800);
        assertFinalBalanceForAccount(a2, 800);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -100   | +150 -> A2
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t14 | 12:30 | +100   | +1000 <- A1
        // t22 | 13:00 | -100   | +900
        Transaction t14 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12,30,0,0)).create();
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
    }

    public void test_should_update_running_balance_when_updating_amount_on_existing_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -100   | +150 -> A2
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t14 | 12:30 | +100   | +1000 <- A1
        // t22 | 13:00 | -100   | +900
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t12 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11,5,0,0)).create();
        Transaction t13 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12,0,0,0)).create();
        Account a2 = AccountBuilder.createDefault(db);
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t22 = TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13,0,0,0)).create();
        Transaction t14 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12,30,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        db.rebuildRunningBalanceForAccount(a2);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -200   | +50 -> A2 <- update amount
        // A2  | time  | amount | balance
        // t21 | 11:00 | +900   | +900
        // t14 | 12:30 | +250   | +1150 <- A1 <- update amount
        // t22 | 13:00 | -100   | +1050
        t14.fromAmount = -200;
        t14.toAmount = +250;
        db.insertOrUpdate(t14);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 50);
        assertFinalBalanceForAccount(a1, 50);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1150);
        assertAccountBalanceForTransaction(t22, a2, 1050);
        assertFinalBalanceForAccount(a2, 1050);
    }

    public void test_should_update_running_balance_when_updating_datetime_on_existing_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t13 | 12:00 | -250   | +250
        // t14 | 12:30 | -100   | +150 -> A2
        // A2  | time  | amount | balance
        // t21 | 12:00 | +900   | +900
        // t14 | 12:30 | +100   | +1000 <- A1
        // t22 | 13:00 | -100   | +900
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Transaction t12 = TransactionBuilder.withDb(db).account(a1).amount(-500).dateTime(DateTime.today().at(11,5,0,0)).create();
        Transaction t13 = TransactionBuilder.withDb(db).account(a1).amount(-250).dateTime(DateTime.today().at(12,0,0,0)).create();
        Account a2 = AccountBuilder.createDefault(db);
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(900).dateTime(DateTime.today().at(12,0,0,0)).create();
        Transaction t22 = TransactionBuilder.withDb(db).account(a2).amount(-100).dateTime(DateTime.today().at(13,0,0,0)).create();
        Transaction t14 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12,30,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        db.rebuildRunningBalanceForAccount(a2);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t13, a1, 250);
        assertAccountBalanceForTransaction(t14, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t21, a2, 900);
        assertAccountBalanceForTransaction(t14, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 11:05 | -500   | +500
        // t14 | 11:10 | -100   | +400 -> A2 <- move up
        // t13 | 12:00 | -250   | +150
        // A2  | time  | amount | balance
        // t14 | 11:10 | +100   | +100 <- A1
        // t21 | 12:00 | +900   | +1000
        // t22 | 13:00 | -100   | +900
        t14.dateTime = DateTime.today().at(11,10,0,0).asLong();
        db.insertOrUpdate(t14);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 500);
        assertAccountBalanceForTransaction(t14, a1, 400);
        assertAccountBalanceForTransaction(t13, a1, 150);
        assertFinalBalanceForAccount(a1, 150);
        assertAccountBalanceForTransaction(t14, a2, 100);
        assertAccountBalanceForTransaction(t21, a2, 1000);
        assertAccountBalanceForTransaction(t22, a2, 900);
        assertFinalBalanceForAccount(a2, 900);
    }

    public void test_should_update_running_balance_when_updating_account_on_existing_transfer() {
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 12:30 | -100   | +900 -> A2
        // A2  | time  | amount | balance
        // t21 | 12:00 | +500   | +500
        // t12 | 12:30 | +100   | +600 <- A1
        // A3  | time  | amount | balance
        // t31 | 13:00 | +100   | +100
        Account a1 = AccountBuilder.createDefault(db);
        Transaction t11 = TransactionBuilder.withDb(db).account(a1).amount(1000).dateTime(DateTime.today().at(11,0,0,0)).create();
        Account a2 = AccountBuilder.createDefault(db);
        Transaction t21 = TransactionBuilder.withDb(db).account(a2).amount(500).dateTime(DateTime.today().at(12,0,0,0)).create();
        Transaction t12 = TransferBuilder.withDb(db).fromAccount(a1).fromAmount(-100).toAccount(a2).toAmount(100).dateTime(DateTime.today().at(12,30,0,0)).create();
        Account a3 = AccountBuilder.createDefault(db);
        Transaction t31 = TransactionBuilder.withDb(db).account(a3).amount(100).dateTime(DateTime.today().at(13,0,0,0)).create();
        db.rebuildRunningBalanceForAccount(a1);
        db.rebuildRunningBalanceForAccount(a2);
        db.rebuildRunningBalanceForAccount(a3);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 900);
        assertFinalBalanceForAccount(a1, 900);
        assertAccountBalanceForTransaction(t21, a2, 500);
        assertAccountBalanceForTransaction(t12, a2, 600);
        assertFinalBalanceForAccount(a2, 600);
        assertAccountBalanceForTransaction(t31, a3, 100);
        assertFinalBalanceForAccount(a3, 100);
        // A1  | time  | amount | balance
        // t11 | 11:00 | +1000  | +1000
        // t12 | 12:30 | -100   | +900 -> A3 <- update account
        // A2  | time  | amount | balance
        // t21 | 12:00 | +500   | +500
        // A3  | time  | amount | balance
        // t12 | 12:30 | +100   | +100 <- A1
        // t31 | 13:00 | +100   | +200
        t12.toAccountId = a3.id;
        db.insertOrUpdate(t12);
        assertAccountBalanceForTransaction(t11, a1, 1000);
        assertAccountBalanceForTransaction(t12, a1, 900);
        assertFinalBalanceForAccount(a1, 900);
        assertAccountBalanceForTransaction(t21, a2, 500);
        assertFinalBalanceForAccount(a2, 500);
        assertAccountBalanceForTransaction(t12, a3, 100);
        assertAccountBalanceForTransaction(t31, a3, 200);
        assertFinalBalanceForAccount(a3, 200);
    }

    private void assertFinalBalanceForAccount(Account a, long expectedBalance) {
        Cursor c = db.db().rawQuery("select balance from running_balance where account_id=? order by datetime desc limit 1",
                new String[]{String.valueOf(a.id)});
        try {
            if (c.moveToFirst()) {
                long balance = c.getLong(0);
                assertEquals(expectedBalance, balance);
            } else {
                fail();
            }
        } finally {
            c.close();
        }
    }

    private void assertAccountBalanceForTransaction(Transaction t, Account a, long expectedBalance) {
        Cursor c = db.db().rawQuery("select balance from running_balance where account_id=? and transaction_id=?",
                new String[]{String.valueOf(a.id), String.valueOf(t.id)});
        try {
            if (c.moveToFirst()) {
                long balance = c.getLong(0);
                assertEquals(expectedBalance, balance);
            } else {
                fail();
            }
        } finally {
            c.close();
        }
    }


}
