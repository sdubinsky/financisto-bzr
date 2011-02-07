package ru.orangesoftware.financisto.export.qif;

import android.database.Cursor;

import java.io.IOException;
import java.math.BigDecimal;
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
    private static final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

    public Date date;
    public BigDecimal amount;

    public static QifTransaction fromBlotterCursor(Cursor c) {
        QifTransaction t = new QifTransaction();
        t.date = new Date(c.getLong(datetime.ordinal()));
        t.amount = new BigDecimal(c.getLong(from_amount.ordinal())).setScale(2);
        return t;
    }

    public void writeTo(QifBufferedWriter qifWriter) throws IOException {
        qifWriter.write("D").write(df.format(date)).newLine();
        qifWriter.write("T").write(amount.toString()).newLine();
        qifWriter.end();
    }

}
