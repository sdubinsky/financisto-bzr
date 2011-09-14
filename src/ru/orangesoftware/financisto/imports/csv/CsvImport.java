package ru.orangesoftware.financisto.imports.csv;

import static ru.orangesoftware.financisto.db.DatabaseHelper.ACCOUNT_TABLE;
import static ru.orangesoftware.financisto.db.DatabaseHelper.V_BLOTTER_FOR_ACCOUNT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;


import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.AccountColumns;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.export.csv.Csv;
import ru.orangesoftware.financisto.model.Transaction;

public class CsvImport {
	private final DatabaseAdapter db;
    private final CsvImportOptions options;

	public CsvImport(DatabaseAdapter db, CsvImportOptions options) {
		this.db = db;
		this.options = options;
	}

	static final HashMap<String , String> mapCsvIngDibaToTransaction = new HashMap<String , String>() {{
    	put("Buchung","date");
    	put("Valuta","");
    	put("Auftraggeber/Empfänger","payee");
    	put("Buchungstext","");
    	put("Verwendungszweck","note");
    	put("Betrag","amount");
    	put("Währung","currency");
    	put("Saldo",""); //Gesamtbetrag
    	put("Währung","currency");
    }};
    
    public Object doImport(){
    	String csvFilename=options.filename;
    	Csv.Reader reader;
    	Boolean isTableHeadLine=false;
    	List<String> tableHeadLine=null;
    	Transaction transaction=new Transaction();
    	
		try {
			reader = new Csv.Reader(new FileReader(csvFilename)).delimiter(options.fieldSeparator).ignoreComments(true);
			List<String> line;
			while ((line=reader.readLine())!=null){
				//search for table head line
				
				if (isTableHeadLine){
					transaction.accuracy=(float) 0.0;
					transaction.attachedPicture=null;
					transaction.categoryId=0;
					transaction.fromAccountId=options.selectedAccounts[0];
					transaction.id=-1;
					transaction.isCCardPayment=0;	
					transaction.isTemplate=0;	
					transaction.latitude=0.0;	
					transaction.locationId=0;	
					transaction.longitude=.0;	
					transaction.notificationOptions=null;	
					transaction.projectId=0;	
					transaction.provider=null;	
					transaction.recurrence=null;	
					transaction.templateName=null;	
					transaction.toAccountId=0;	
					transaction.toAmount=0;
					transaction.fromAmount=0;
					transaction.note="";
					transaction.dateTime=0;
					
					
					
					
					int countOfColumns = line.size();
					for(int i=0;i<countOfColumns;i++){
						String transactionField = tableHeadLine.get(i);
						if (!transactionField.equals("")){
							Log.i("CsvImport",transactionField+":"+line.get(i)+" ");
							try {
								String fieldValue = line.get(i);
								if (transactionField.equals("date")){
									Date date = options.dateFormat.parse(fieldValue);
									transaction.dateTime=date.getTime();
								} else if (transactionField.equals("time")){
									//ToDo
								} else if (transactionField.equals("amount")){
									fieldValue=fieldValue.replace(",", ".");
									double fromAmount=Double.parseDouble(fieldValue);
									Double fromAmountDouble=new Double(fromAmount)*100.0;
									long amount = fromAmountDouble.longValue();
									transaction.fromAmount=amount;
								}else if (transactionField.equals("payee")){
									transaction.payeeId=db.insertPayee(fieldValue);
								}else if (transactionField.equals("category")){
									//ToDo
								}else if (transactionField.equals("note")){
									transaction.note=fieldValue;
								}else if (transactionField.equals("project")){
									//ToDo
								}  
								
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return null;
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								return null;
							}
						}
					}
					long id = db.insertOrUpdate(transaction);
					Log.i("CsvImport","Insert transactionId:"+id);
					
				}else{
				//first line of csv-file is table headline
					isTableHeadLine=true;
					tableHeadLine=line;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    	   return null; 	
    }
}
	

