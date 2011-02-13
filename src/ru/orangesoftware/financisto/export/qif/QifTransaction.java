package ru.orangesoftware.financisto.export.qif;

import android.database.Cursor;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 12:52 AM
 */
public class QifTransaction {

    // this class must not be used by multiple threads
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private HashMap<Long, Category> categoriesMap;

    private Date date;
    private long amount;
    private String payee;
    private String memo;
    private long categoryId;

    public static QifTransaction fromBlotterCursor(Cursor c) {
        QifTransaction t = new QifTransaction();
        t.date = new Date(c.getLong(BlotterColumns.datetime.ordinal()));
        t.amount = c.getLong(BlotterColumns.from_amount.ordinal());
        t.payee = c.getString(BlotterColumns.payee.ordinal());
        t.memo = c.getString(BlotterColumns.note.ordinal());
        t.categoryId = c.getLong(BlotterColumns.category_id.ordinal());
        return t;
    }

    public void writeTo(QifBufferedWriter qifWriter) throws IOException {
        qifWriter.write("D").write(DATE_FORMAT.format(date)).newLine();
        qifWriter.write("T").write(Utils.amountToString(Currency.EMPTY, amount)).newLine();
        if (categoryId > 0) {
            qifWriter.write("L").write(buildCategoryPath(categoryId)).newLine();
        }
        if (Utils.isNotEmpty(payee)) {
            qifWriter.write("P").write(payee).newLine();
        }
        if (Utils.isNotEmpty(memo)) {
            qifWriter.write("M").write(memo).newLine();
        }
        qifWriter.end();
    }

    private String buildCategoryPath(long categoryId) {
        Category c = getCategoryById(categoryId);
        if (c != null) {
            StringBuilder sb = new StringBuilder(c.title);
			for (Category cat = c.parent; cat != null; cat = cat.parent) {
                sb.insert(0,"/").insert(0, cat.title);
			}
			return sb.toString();
        }
        return "";
    }

    private Category getCategoryById(long categoryId) {
        return categoriesMap.get(categoryId);
    }

    public void userCategoriesCache(HashMap<Long, Category> categoriesMap) {
        this.categoriesMap = categoriesMap;
    }

}
