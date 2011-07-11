package ru.orangesoftware.financisto.export.qif;

import android.database.Cursor;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 12:52 AM
 */
public class QifTransaction {

    private HashMap<Long, Category> categoriesMap;

    private Date date;
    private long amount;
    private String payee;
    private String memo;
    private long categoryId;
    private long toAccountId;
    private String toAccountTitle;

    public static QifTransaction fromBlotterCursor(Cursor c) {
        QifTransaction t = new QifTransaction();
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
        qifWriter.end();
    }

    private Category getCategoryById(long categoryId) {
        return categoriesMap.get(categoryId);
    }

    public void userCategoriesCache(HashMap<Long, Category> categoriesMap) {
        this.categoriesMap = categoriesMap;
    }

}
