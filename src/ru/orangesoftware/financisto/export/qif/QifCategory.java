package ru.orangesoftware.financisto.export.qif;

import ru.orangesoftware.financisto.model.Category;

import java.io.IOException;

import static ru.orangesoftware.financisto.export.qif.QifUtils.trimFirstChar;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/16/11 10:08 PM
 */
public class QifCategory {

    public static final String SEPARATOR = ":";

    public String name;
    public boolean isIncome = false;

    public QifCategory() {
    }

    public QifCategory(String name, boolean income) {
        this.name = name;
        this.isIncome = income;
    }

    public static QifCategory fromCategory(Category c) {
        QifCategory qifCategory = new QifCategory();
        qifCategory.name = buildName(c);
        qifCategory.isIncome = c.isIncome();
        return qifCategory;
    }

    private static String buildName(Category c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.title);
        for (Category p = c.parent; p != null; p = p.parent) {
            sb.insert(0, SEPARATOR);
            sb.insert(0, p.title);
        }
        return sb.toString();
    }

    public void writeTo(QifBufferedWriter qifWriter) throws IOException {
        qifWriter.write("N").write(name).newLine();
        qifWriter.write(isIncome ? "I" : "E").newLine();
        qifWriter.end();
    }

    public void readFrom(QifBufferedReader r) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("^")) {
                break;
            }
            if (line.startsWith("N")) {
                this.name = trimFirstChar(line);
            } else if (line.startsWith("I")) {
                this.isIncome = true;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QifCategory that = (QifCategory) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "{"+name+"("+(isIncome?"I":"E")+"}";
    }
}
