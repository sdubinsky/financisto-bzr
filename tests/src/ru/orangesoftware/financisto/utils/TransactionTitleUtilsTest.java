package ru.orangesoftware.financisto.utils;

import android.test.AndroidTestCase;

import static ru.orangesoftware.financisto.utils.TransactionTitleUtils.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/18/11 7:15 PM
 */
public class TransactionTitleUtilsTest extends AndroidTestCase {

    public void test_should_generate_title_for_regular_transactions() {
        assertEquals("", generateTransactionTitle(sb(), null, null, null, null));
        assertEquals("Payee", generateTransactionTitle(sb(), "Payee", null, null, null));
        assertEquals("Note", generateTransactionTitle(sb(), null, "Note", null, null));
        assertEquals("Location", generateTransactionTitle(sb(), null, null, "Location", null));
        assertEquals("Category", generateTransactionTitle(sb(), null, null, null, "Category"));
        assertEquals("Payee: Location: Note", generateTransactionTitle(sb(), "Payee", "Note", "Location", null));
        assertEquals("Category (Location)", generateTransactionTitle(sb(), null, null, "Location", "Category"));
        assertEquals("Category (Payee: Note)", generateTransactionTitle(sb(), "Payee", "Note", null, "Category"));
    }

    public void test_should_generate_title_for_a_split() {
        assertEquals("[Split...]", generateTransactionTitleForSplit(sb(), null, null, null, "[Split...]"));
        assertEquals("[Payee...]", generateTransactionTitleForSplit(sb(), "Payee", null, null, "[Split...]"));
        assertEquals("[...] Note", generateTransactionTitleForSplit(sb(), null, "Note", null, "[Split...]"));
        assertEquals("[...] Location", generateTransactionTitleForSplit(sb(), null, null, "Location", "[Split...]"));
        assertEquals("[Payee...] Location: Note", generateTransactionTitleForSplit(sb(), "Payee", "Note", "Location", "[Split...]"));
        assertEquals("[...] Location", generateTransactionTitleForSplit(sb(), null, null, "Location", "[Split...]"));
        assertEquals("[Payee...] Note", generateTransactionTitleForSplit(sb(), "Payee", "Note", null, "[Split...]"));
    }

    private StringBuilder sb() {
        return new StringBuilder();
    }

}
