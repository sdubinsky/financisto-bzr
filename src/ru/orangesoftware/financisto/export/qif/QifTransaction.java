package ru.orangesoftware.financisto.export.qif;

import android.database.Cursor;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns.*;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/8/11 12:52 AM
 */
public class QifTransaction {

    // this class must not be used by multiple threads
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public Date date;
    public long amount;

    public static QifTransaction fromBlotterCursor(Cursor c) {
        QifTransaction t = new QifTransaction();
        t.date = new Date(c.getLong(datetime.ordinal()));
        t.amount = c.getLong(from_amount.ordinal());
        return t;
    }

    public void writeTo(QifBufferedWriter qifWriter) throws IOException {
        qifWriter.write("D").write(DATE_FORMAT.format(date)).newLine();
        qifWriter.write("T").write(Utils.amountToString(Currency.EMPTY, amount)).newLine();
        qifWriter.end();
    }

}
