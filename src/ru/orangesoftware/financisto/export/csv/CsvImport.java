package ru.orangesoftware.financisto.export.csv;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.export.CategoryCache;
import ru.orangesoftware.financisto.export.CategoryInfo;
import ru.orangesoftware.financisto.export.ProgressListener;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.Utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

public class CsvImport {

    private final DatabaseAdapter db;
    private final CsvImportOptions options;
    private final Account account;
    private char decimalSeparator;
    private char groupSeparator;
    private ProgressListener progressListener;

    public CsvImport(DatabaseAdapter db, CsvImportOptions options) {
        this.db = db;
        this.options = options;
        this.account = db.em().getAccount(options.selectedAccountId);
        this.decimalSeparator = options.currency.decimalSeparator.charAt(1);
        this.groupSeparator = options.currency.groupSeparator.charAt(1);
    }

    public Object doImport() throws Exception {
        long t0 = System.currentTimeMillis();
        List<CsvTransaction> transactions = parseTransactions();
        long t1 = System.currentTimeMillis();
        Log.i("Financisto", "Parsing transactions ="+(t1-t0)+"ms");
        Map<String, Category> categories = collectAndInsertCategories(transactions);
        long t2 = System.currentTimeMillis();
        Log.i("Financisto", "Collecting categories ="+(t2-t1)+"ms");
        Map<String, Project> projects = collectAndInsertProjects(transactions);
        long t3 = System.currentTimeMillis();
        Log.i("Financisto", "Collecting projects ="+(t3-t2)+"ms");
        Map<String, Payee> payees = collectAndInsertPayees(transactions);
        long t4 = System.currentTimeMillis();
        Log.i("Financisto", "Collecting payees ="+(t4-t3)+"ms");
        importTransactions(transactions, categories, projects, payees);
        long t5 = System.currentTimeMillis();
        Log.i("Financisto", "Inserting transactions ="+(t5-t4)+"ms");
        Log.i("Financisto", "Overall csv import ="+((t5-t0)/1000)+"s");
        return options.filename + " imported!";
    }

    public Map<String, Project> collectAndInsertProjects(List<CsvTransaction> transactions) {
        MyEntityManager em = db.em();
        Map<String, Project> map = entitiesAsMap(em.getAllProjectsList(false));
        for (CsvTransaction transaction : transactions) {
            String project = transaction.project;
            if (isNewProject(map, project)) {
                Project p = new Project();
                p.title = project;
                p.isActive = true;
                em.saveOrUpdate(p);
                map.put(project, p);
            }
        }
        return map;
    }

    private boolean isNewProject(Map<String, Project> map, String project) {
        return Utils.isNotEmpty(project) && !"No project".equals(project) && !map.containsKey(project);
    }

    public Map<String, Payee> collectAndInsertPayees(List<CsvTransaction> transactions) {
        MyEntityManager em = db.em();
        Map<String, Payee> map = entitiesAsMap(em.getAllPayeeList());
        for (CsvTransaction transaction : transactions) {
            String payee = transaction.payee;
            if (isNewPayee(map, payee)) {
                Payee p = new Payee();
                p.title = payee;
                em.saveOrUpdate(p);
                map.put(payee, p);
            }
        }
        return map;
    }

    private static <T extends MyEntity> Map<String, T> entitiesAsMap(List<T> entities) {
        Map<String, T> map = new HashMap<String, T>();
        for (T e: entities) {
            map.put(e.title, e);
        }
        return map;
    }

    private boolean isNewPayee(Map<String, Payee> map, String payee) {
        return Utils.isNotEmpty(payee) && !map.containsKey(payee);
    }

    public Map<String, Category> collectAndInsertCategories(List<CsvTransaction> transactions) {
        Set<CategoryInfo> categories = collectCategories(transactions);
        CategoryCache cache = new CategoryCache();
        cache.loadExistingCategories(db);
        cache.insertCategories(db, categories);
        return cache.categoryNameToCategory;
    }

    private void importTransactions(List<CsvTransaction> transactions, 
                                    Map<String, Category> categories, 
                                    Map<String, Project> projects,
                                    Map<String, Payee> payees) {
        SQLiteDatabase database = db.db();
        database.beginTransaction();
        try {
            List<TransactionAttribute> emptyAttributes = Collections.emptyList();
            int count = 0;
            int totalCount = transactions.size();
            for (CsvTransaction transaction : transactions) {
                Transaction t = transaction.createTransaction(categories, projects, payees);
                db.insertOrUpdateInTransaction(t, emptyAttributes);
                if (++count % 100 == 0) { 
                    Log.i("Financisto", "Inserted "+count+" out of "+totalCount);
                    if (progressListener != null) {
                        progressListener.onProgress((int)(100f*count/totalCount));
                    }
                }
            }
            Log.i("Financisto", "Total transactions inserted: "+count);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private List<CsvTransaction> parseTransactions() throws Exception {
        String csvFilename = options.filename;
        boolean parseLine = false;
        List<String> header = null;
        if (!options.useHeaderFromFile) {
            parseLine = true;
            header = Arrays.asList(CsvExport.HEADER);
        }
        try {
            long deltaTime = 0;
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
            Csv.Reader reader = new Csv.Reader(new FileReader(csvFilename))
                    .delimiter(options.fieldSeparator).ignoreComments(true);
            List<CsvTransaction> transactions = new LinkedList<CsvTransaction>();
            List<String> line;
            while ((line = reader.readLine()) != null) {
                if (parseLine) {
                    CsvTransaction transaction = new CsvTransaction();
                    transaction.fromAccountId = this.account.id;
                    int countOfColumns = line.size();
                    for (int i = 0; i < countOfColumns; i++) {
                        String transactionField = myTrim(header.get(i));
                        if (!transactionField.equals("")) {
                            try {
                                String fieldValue = line.get(i);
                                if (!fieldValue.equals("")) {
                                    if (transactionField.equals("date")) {
                                        transaction.date = options.dateFormat.parse(fieldValue).getTime();
                                    } else if (transactionField.equals("time")) {
                                        ParsePosition p = new ParsePosition(0);
                                        transaction.time = format.parse(fieldValue, p).getTime();
                                    } else if (transactionField.equals("amount")) {
                                        fieldValue = fieldValue.replace(groupSeparator + "", "");
                                        fieldValue = fieldValue.replace(decimalSeparator, '.');
                                        double fromAmount = Double.parseDouble(fieldValue);
                                        Double fromAmountDouble = fromAmount * 100.0;
                                        transaction.fromAmount = fromAmountDouble.longValue();
                                    } else if (transactionField.equals("payee")) {
                                        transaction.payee = fieldValue;
                                    } else if (transactionField.equals("category")) {
                                        transaction.category = fieldValue;
                                    } else if (transactionField.equals("parent")) {
                                        transaction.categoryParent = fieldValue;
                                    } else if (transactionField.equals("note")) {
                                        transaction.note = fieldValue;
                                    } else if (transactionField.equals("project")) {
                                        transaction.project = fieldValue;
                                    } else if (transactionField.equals("currency")) {
                                        if (!account.currency.name.equals(fieldValue)) {
                                            throw new Exception("Wrong currency "+fieldValue);
                                        }
                                        transaction.currency = fieldValue;
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                throw new Exception("IllegalArgumentException");
                            } catch (ParseException e) {
                                throw new Exception("ParseException");
                            }
                        }
                    }
                    transaction.time += deltaTime++;
                    transactions.add(transaction);
                } else {
                    // first line of csv-file is table headline
                    parseLine = true;
                    header = line;
                }
            }
            return transactions;
        } catch (FileNotFoundException e) {
            throw new Exception("Import file not found");
        }
    }

    public Set<CategoryInfo> collectCategories(List<CsvTransaction> transactions) {
        Set<CategoryInfo> categories = new HashSet<CategoryInfo>();
        for (CsvTransaction transaction : transactions) {
            String category = transaction.category;
            if (Utils.isNotEmpty(transaction.categoryParent)) {
                category = transaction.categoryParent+CategoryInfo.SEPARATOR+category;
            }
            if (Utils.isNotEmpty(category)) {
                categories.add(new CategoryInfo(category, false));
                transaction.category = category;
                transaction.categoryParent = null;
            }
        }
        return categories;
    }

    //Workaround function which is needed for reimport of CsvExport files
    public String myTrim(String s) {
        if (Character.isLetter(s.charAt(0))) {
            return s;
        } else {
            return s.substring(1);
        }

    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }
}
