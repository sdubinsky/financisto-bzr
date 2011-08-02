package ru.orangesoftware.financisto.export.qif;

import android.database.Cursor;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 12:52 AM
 */
public class QifTransaction {

    private Map<Long, Category> categoriesMap;
    private Map<Long, Account> accountsMap;

    public long id;
    private Date date;
    private long amount;
    private String payee;
    private String memo;
    private long categoryId;
    private long toAccountId;
    private String toAccountTitle;
    private List<Transaction> splits;

    public static QifTransaction fromBlotterCursor(Cursor c) {
        QifTransaction t = new QifTransaction();
        t.id = c.getLong(BlotterColumns._id.ordinal());
        t.date = new Date(c.getLong(BlotterColumns.datetime.ordinal()));
        t.amount = c.getLong(BlotterColumns.from_amount.ordinal());
        t.payee = c.getString(BlotterColumns.payee.ordinal());
        t.memo = c.getString(BlotterColumns.note.ordinal());
        t.categoryId = c.getLong(BlotterColumns.category_id.ordinal());
        t.toAccountId = c.getLong(BlotterColumns.to_account_id.ordinal());
        t.toAccountTitle = c.getString(BlotterColumns.to_account_title.ordinal());
        return t;
    }

    public void writeTo(QifBufferedWriter qifWriter, QifExportOptions options) throws IOException {
        qifWriter.write("D").write(options.dateFormat.format(date)).newLine();
        qifWriter.write("T").write(Utils.amountToString(options.currency, amount)).newLine();
        if (toAccountId > 0) {
            qifWriter.write("L[").write(toAccountTitle).write("]").newLine();
        } else if (categoryId > 0) {
            QifCategory qifCategory = QifCategory.fromCategory(getCategoryById(categoryId));
            qifWriter.write("L").write(qifCategory.name).newLine();
        }
        if (Utils.isNotEmpty(payee)) {
            qifWriter.write("P").write(payee).newLine();
        }
        if (Utils.isNotEmpty(memo)) {
            qifWriter.write("M").write(memo).newLine();
        }
        if (isSplit()) {
            for (Transaction split : splits) {
                writeSplit(qifWriter, options, split);
            }
        }
        qifWriter.end();
    }

    private void writeSplit(QifBufferedWriter qifWriter, QifExportOptions options, Transaction split) throws IOException {
        if (split.isTransfer()) {
            Account toAccount = accountsMap.get(split.toAccountId);
            qifWriter.write("S[").write(toAccount.title).write("]").newLine();
        } else {
            if (split.categoryId > 0) {
                QifCategory qifCategory = QifCategory.fromCategory(getCategoryById(split.categoryId));
                qifWriter.write("S").write(qifCategory.name).newLine();
            } else {
                qifWriter.write("S<NO_CATEGORY>").newLine();
            }
        }
        qifWriter.write("$").write(Utils.amountToString(options.currency, split.fromAmount)).newLine();
        if (Utils.isNotEmpty(split.note)) {
            qifWriter.write("E").write(split.note).newLine();
        }
    }

    private Category getCategoryById(long categoryId) {
        return categoriesMap.get(categoryId);
    }

    public void useCategoriesCache(Map<Long, Category> categoriesMap) {
        this.categoriesMap = categoriesMap;
    }

    public void useAccountsCache(Map<Long, Account> accountsMap) {
        this.accountsMap = accountsMap;
    }

    public boolean isSplit() {
        return categoryId == Category.SPLIT_CATEGORY_ID;
    }

    public void setSplits(List<Transaction> splits) {
        this.splits = splits;
    }

}
