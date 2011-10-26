package ru.orangesoftware.financisto.imports.csv;

import android.util.Log;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.csv.Csv;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.Transaction;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.List;

public class CsvImport {

    private final DatabaseAdapter db;
    private final CsvImportOptions options;
    private final Account account;
    private char decimalSeparator;
    private char groupSeparator;

    public CsvImport(DatabaseAdapter db, CsvImportOptions options) {
        this.db = db;
        this.options = options;
        this.account = db.em().getAccount(options.selectedAccounts[0]);
        this.decimalSeparator = options.currency.decimalSeparator.charAt(1);
        this.groupSeparator = options.currency.groupSeparator.charAt(1);
    }

    public Object doImport() throws Exception {
        String csvFilename = options.filename;
        Csv.Reader reader;
        Boolean isTableHeadLine = false;
        List<String> tableHeadLine = null;
        List<Project> projectList = db.em().list(Project.class);
        try {
            reader = new Csv.Reader(new FileReader(csvFilename)).delimiter(
                    options.fieldSeparator).ignoreComments(true);
            List<String> line;
            int countLine = 0;
            while ((line = reader.readLine()) != null) {
                // get table head line
                countLine = countLine++;
                if (isTableHeadLine) {
                    Transaction transaction = new Transaction();
                    transaction.dateTime = 0;
                    transaction.fromAccountId = this.account.id;
                    long time = 0;

                    int countOfColumns = line.size();
                    for (int i = 0; i < countOfColumns; i++) {
                        String transactionField = tableHeadLine.get(i);
                        /*
                               * ToDo:workaround needed for reimport of files from CsvExport function - first column (date) in header shows nonprintable
                               * Character which is not replaceable by "trim" function. Have to look in CsvExport function
                               */
                        transactionField = myTrim(transactionField);
                        if (!transactionField.equals("")) {
                            Log.d("CsvImport", transactionField + ":" + line.get(i) + " ");
                            try {
                                String fieldValue = line.get(i);
                                if (!fieldValue.equals("")) {
                                    if (transactionField.equals("date")) {
                                        transaction.dateTime = options.dateFormat.parse(fieldValue).getTime();
                                        Log.d("CsvImport", "date:" + transaction.dateTime);
                                    } else if (transactionField.equals("time")) {
                                        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                                        ParsePosition p = new ParsePosition(0);
                                        time = format.parse(fieldValue, p).getTime();
                                        Log.d("CsvImport", "time:" + time);

                                    } else if (transactionField.equals("amount")) {
                                        fieldValue = fieldValue.replace(groupSeparator + "", "");
                                        fieldValue = fieldValue.replace(decimalSeparator, '.');
                                        double fromAmount = Double.parseDouble(fieldValue);
                                        Double fromAmountDouble = fromAmount * 100.0;
                                        transaction.fromAmount = fromAmountDouble.longValue();
                                    } else if (transactionField.equals("payee")) {
                                        transaction.payeeId = db.insertPayee(fieldValue);
                                    } else if (transactionField.equals("category")) {
                                        Category cat = db.getCategory(fieldValue);
                                        if (cat != null) {
                                            transaction.categoryId = cat.id;
                                        } else {
                                            throw new Exception("Unknown category in import line");
                                        }
                                    } else if (transactionField.equals("note")) {
                                        transaction.note = fieldValue;
                                    } else if (transactionField.equals("project")) {
                                        //Test for "No project" just to be compatible for importing of files from CsvExport function
                                        if (!fieldValue.equals("No project")) {
                                            for (Project project : projectList) {
                                                if (project.getTitle().equals(fieldValue)) {
                                                    transaction.projectId = project.getId();
                                                    break;
                                                }
                                            }
                                            if (transaction.projectId == 0) {
                                                throw new Exception("Unknown project in import line");
                                            }
                                        }

                                    } else if (transactionField.equals("currency")) {
                                        if (!account.currency.name.equals(fieldValue)) {
                                            throw new Exception("Wrong currency in import line");
                                        }
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                throw new Exception("IllegalArgumentException");
                            } catch (ParseException e) {
                                throw new Exception("ParseException");
                            }
                        }
                    }
                    transaction.dateTime = transaction.dateTime + time;
                    long id = db.insertOrUpdate(transaction);
                    Log.d("CsvImport", "Insert transactionId:" + id);
                } else {
                    // first line of csv-file is table headline
                    isTableHeadLine = true;
                    tableHeadLine = line;
                }
            }
        } catch (FileNotFoundException e) {
            throw new Exception("Import file not found");
        }

        return options.filename + " imported!";
    }

    //Workaround function which is needed for reimport of CsvExport files
    public String myTrim(String s) {
        if (Character.isLetter(s.charAt(0))) {
            return s;
        } else {
            return s.substring(1);
        }

    }

}
