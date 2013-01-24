package ru.orangesoftware.financisto.export;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSync;
import ru.orangesoftware.financisto.export.flowzr.FlowzrSyncOptions;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.model.Payee;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.model.Transaction;
import ru.orangesoftware.orb.EntityManager;


public class FlowzrSyncTest extends AbstractDbTest {

	FlowzrSync flowzrSync;
	EntityManager em;

	Currency currency1;
	Currency currency2;	
	
	public void setUp() throws Exception {
		super.setUp();
		em=db.em();
		FlowzrSyncOptions options=new FlowzrSyncOptions("", 0, null);
		flowzrSync=new FlowzrSync(getContext(), db, options, null);
		doCurrencyFromJSON();			
		
		category1=doCategoryFromJSON();
		project1=doProjectFromJSON();
		payee1=doPayeeFromJSON();
		account1=doAccountFromJSON();
		location1=doLocationFromJSON();	
	}
	
	final String JSONCUR1="" +
			"{\"name\": \"EUR\", " +
			"\"decimalSeparator\": \"','\", " +
			"\"title\": \"Euro\", " +
			"\"symbol\": \"\u20ac\", " +
			"\"groupSeparator\": \"' '\", " +
			"\"key\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyEAsSCEN1cnJlbmN5GOPTBgyiARJjb2xtZWRpc19nbWFpbC5jb20\", " +
			"\"updated_on\": 1349967700, " +
			"\"isDefault\": true} ";
			
	public void testCurrencyFromJSON() {
		assertEquals("privacy_gmail.com::agxzfmZsb3d6ci1ocmRyEAsSCEN1cnJlbmN5GOPTBgyiARJjb2xtZWRpc19nbWFpbC5jb20", currency1.remoteKey);
		assertEquals("EUR", currency1.name);
		assertEquals("','", currency1.decimalSeparator);
		assertEquals("Euro", currency1.title);
		assertEquals("€", currency1.symbol);		
		assertEquals("' '", currency1.groupSeparator);		
		assertEquals(true, currency1.isDefault);	
	}
	
	public Currency doCurrencyFromJSON() {
		JSONObject jsonObj=null;
		try {			
			jsonObj = new JSONObject( new JSONTokener(JSONCUR1) );
		} catch (JSONException e) {
			fail("parse json failed");
		}
		currency1=(Currency)flowzrSync.saveOrUpdateCurrencyFromJSON(-1,jsonObj);				
		return currency1;
	}
	
	final String JSONCAT1="{" +
			"\"owner\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEgsSCkNyZWJpdFVzZXIY--8hDKIBC2NyZWJpdHMtYWFh\", " +
			"\"updated_on\": 1349967701, " +
			"\"name\": \"a category\", " +
			"\"key\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEAsSCENhdGVnb3J5GOuKIQyiAQtjcmViaXRzLWFhYQ\"" +
			"}";

	Category category1;
	
	public void testCategoryFromJSON() {
        assertEquals("crebits-aaa::agxzfmZsb3d6ci1ocmRyEAsSCENhdGVnb3J5GOuKIQyiAQtjcmViaXRzLWFhYQ", category1.remoteKey);
		assertEquals("a category", category1.title);
		assertEquals(true, category1.id>=0);
		//TODO test type:expense/income,left/right
	}
	
	
	public Category doCategoryFromJSON() {
		JSONObject jsonObj=null;	
		try {			
			jsonObj = new JSONObject( new JSONTokener(JSONCAT1) );
		} catch (JSONException e) {
			fail("parse json failed");
		}						
		return (Category)flowzrSync.saveOrUpdateEntityFromJSON(Category.class,-1,jsonObj);
	}

	Project project1;
	String JSONPROJECT="{" +
			"\"owner\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEgsSCkNyZWJpdFVzZXIY--8hDKIBC2NyZWJpdHMtYWFh\", " +
			"\"updated_on\": 1349967702, " +
			"\"is_active\": true, " +
			"\"name\": \"a project\", " +
			"\"key\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDwsSB1Byb2plY3QYlvYFDKIBEmNvbG1lZGlzX2dtYWlsLmNvbQ\"}";
	
	public void testProjectFromJSON() {
        assertEquals("privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDwsSB1Byb2plY3QYlvYFDKIBEmNvbG1lZGlzX2dtYWlsLmNvbQ", project1.remoteKey);
		assertEquals("a project", project1.title);
		assertEquals(true, project1.isActive);		
	}
	
	public Project doProjectFromJSON() {
		JSONObject jsonObj=null;	
		try {			
			jsonObj = new JSONObject( new JSONTokener(JSONPROJECT) );
		} catch (JSONException e) {
			fail("parse json failed");
		}						
		return (Project)flowzrSync.saveOrUpdateEntityFromJSON(Project.class,-1,jsonObj);
	}	
		
	Payee payee1;
	String JSNPAYEE="{\"owner\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEgsSCkNyZWJpdFVzZXIY--8hDKIBC2NyZWJpdHMtYWFh\", " +
			"\"updated_on\": 1349967703, " +
			"\"name\": \"A Payee\", " +
			"\"key\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDQsSBVBheWVlGOWFBgyiARJjb2xtZWRpc19nbWFpbC5jb20\"}";
	
	public void testPayeeFromJSON() {
        assertEquals("privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDQsSBVBheWVlGOWFBgyiARJjb2xtZWRpc19nbWFpbC5jb20", payee1.remoteKey);
		assertEquals("A Payee", payee1.title);
	}
	
	public Payee doPayeeFromJSON() {
		JSONObject jsonObj=null;		
		try {			
			jsonObj = new JSONObject( new JSONTokener(JSNPAYEE) );
		} catch (JSONException e) {
			fail("parse json failed");
		}
		return (Payee)flowzrSync.saveOrUpdateEntityFromJSON(Payee.class,-1,jsonObj);
	}

	Account account1;
	String JSNACCOUNT="{\"key\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyDwsSB0FjY291bnQYreghDKIBC2NyZWJpdHMtYWFh\", " +
			"\"description\": \"account note\", " +
			"\"currency_id\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyEAsSCEN1cnJlbmN5GOPTBgyiARJjb2xtZWRpc19nbWFpbC5jb20\", " +
			"\"owner\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEgsSCkNyZWJpdFVzZXIY--8hDKIBC2NyZWJpdHMtYWFh\", " +
			"\"created_on\": 1349913600, " +
			"\"card_issuer\": \"VISA\", " +
			"\"closed\": false, " +
			"\"dateOfLastTransaction\": 1349913600, " +
			"\"updated_on\": 1349967812, " +

			"\"sort_order\": 3, " +
			"\"closing_day\": 3, " +
			"\"payment_day\": 3, " +
			"\"issuer\": \"issuer1\"," +
			"\"total_limit\": 1234.56," +
			"\"card_issuer\": \"MAESTRO\"," +
			"\"iban\": \"43009310000\"," +
			"\"is_include_into_totals\": true, " +			
			
			"\"type\": \"CASH\", " +
			"\"name\": \"accountest\"}";
	
	public void testAccountFromJSON() {
		assertEquals(account1.remoteKey,"crebits-aaa::agxzfmZsb3d6ci1ocmRyDwsSB0FjY291bnQYreghDKIBC2NyZWJpdHMtYWFh");
		assertEquals(account1.note,"account note");	
		assertEquals(account1.creationDate,1349913600);
		assertEquals(account1.cardIssuer,"MAESTRO");		
		assertEquals(account1.isActive,true);
		assertEquals(account1.lastTransactionDate,1349913600);
		assertEquals(account1.isIncludeIntoTotals,true);
		assertEquals(account1.currency,currency1);				
		assertEquals(account1.type,AccountType.CASH.name());	
		assertEquals(account1.sortOrder,3);
		assertEquals(account1.closingDay,3);
		assertEquals(account1.paymentDay,3);		
		assertEquals(account1.issuer,"issuer1");	
		assertEquals(account1.number,"43009310000");	
	}
	
	public Account doAccountFromJSON() {
		  	JSONObject jsonObj=null;
			try {			
				jsonObj = new JSONObject( new JSONTokener(JSNACCOUNT) );
			} catch (JSONException e) {
				fail("parse json failed");
			}	 
			return (Account)flowzrSync.saveOrUpdateAccountFromJSON(-1,jsonObj);
	}
	
	MyLocation location1;
	String JSNLOCATION="{\"name\": \"Position actuelle\", " +
			"\"dateTime\": 1349967704, " +
			"\"geo_point\": {\"lat\": 0.0, \"lon\": 0.0}, " +
			"\"key\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyEAsSCExvY2F0aW9uGPOsBgyiARJjb2xtZWRpc19nbWFpbC5jb20\", " +
			"\"provider\": \"?\", " +
			"\"updated_on\": 1349967704}]}";

	public void testLocationFromJSON() {
	
		assertEquals(location1.name,"Position actuelle");			
		assertEquals(location1.dateTime,1349967704);	
		assertEquals(location1.latitude,0.0);	
		assertEquals(location1.longitude,0.0);
		assertEquals(location1.remoteKey,"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyEAsSCExvY2F0aW9uGPOsBgyiARJjb2xtZWRpc19nbWFpbC5jb20");		
		assertEquals(location1.provider,"?");
		//TODO test location.accuracy,count,isPayee,resolvedAdress
		//assertEquals(location1.accuracy,"?");		
		//assertEquals(location1.count,"?");
		//assertEquals(location1.isPayee);
		//assertEquals(location1.resolvedAddress);
				
	}
	public MyLocation doLocationFromJSON() {
	  	JSONObject jsonObj=null;
		try {			
			jsonObj = new JSONObject( new JSONTokener(JSNLOCATION) );
		} catch (JSONException e) {
			fail("parse json failed");
		}	 
		return (MyLocation)flowzrSync.saveOrUpdateLocationFromJSON(-1,jsonObj);		
	}
	
	Transaction transaction1;
	String JSNTRANSACTION="{" +
			"\"status\": \"UR\", " +
			"\"account\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyDwsSB0FjY291bnQYreghDKIBC2NyZWJpdHMtYWFh\", " +
			"\"to_amount\": 0.0, " +
			"\"dateOfEmission\": 1349913600, " +
			"\"description\": \"Montant initial (accountest)\", " +
			"\"payee_id\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDQsSBVBheWVlGOWFBgyiARJjb2xtZWRpc19nbWFpbC5jb20\", " +
			"\"original_from_amount\": 0.0, " +
			"\"project\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDwsSB1Byb2plY3QYlvYFDKIBEmNvbG1lZGlzX2dtYWlsLmNvbQ\", " +
			"\"geo_point\": {\"lat\": 0.0, \"lon\": 0.0}, " +
			"\"created_on\": 1349913600, " +
			"\"updated_on\": 1349967705, " +
			"\"jived\": false, " +
			"\"key\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDgsSBkNyZWJpdBiczwUMogESY29sbWVkaXNfZ21haWwuY29t\", " +
			"\"debit\": 1000.0, " +
			"\"owner\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEgsSCkNyZWJpdFVzZXIY--8hDKIBC2NyZWJpdHMtYWFh\", " +
			"\"dateOfMaturity\": 1349913600, " +
			"\"cat\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEAsSCENhdGVnb3J5GOuKIQyiAQtjcmViaXRzLWFhYQ\", " +
			"\"location_id\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyEAsSCExvY2F0aW9uGPOsBgyiARJjb2xtZWRpc19nbWFpbC5jb20\"" +
			"}";

	public void testTransactionFromJSON() {
		transaction1=doTransactionFromJSON();
		assertEquals(transaction1.categoryId,category1.id);
		assertEquals(transaction1.projectId,project1.id);
		assertEquals(transaction1.dateTime/1000, 1349913600);
		assertEquals(transaction1.fromAccountId,account1.id);		
		assertEquals(transaction1.payeeId,payee1.id);	
		assertEquals(transaction1.fromAmount,-100000);
		assertEquals(transaction1.note,"Montant initial (accountest)");	
		assertEquals(transaction1.locationId, location1.id);	
		assertEquals(transaction1.categoryId, category1.id);	
		assertEquals(transaction1.remoteKey, "privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDgsSBkNyZWJpdBiczwUMogESY29sbWVkaXNfZ21haWwuY29t");	
	}

	public Transaction doTransactionFromJSON() {
		JSONObject jsonObj=null;
		try {			
				jsonObj = new JSONObject( new JSONTokener(JSNTRANSACTION) );
		} catch (JSONException e) {
				fail("parse json failed");
		}	 
		return (Transaction)flowzrSync.saveOrUpdateTransactionFromJSON(-1,jsonObj);		
	}


	Budget budget1;
	String JSNBUDGET="{" +
			"\"key\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDgsSBkJ1ZGdldBj7mQcMogESY29sbWVkaXNfZ21haWwuY29t\", " +
			"\"startDate\": 1349827200, " +
			"\"endDate\": 1352592000, " +
			"\"includeSubcategories\": true, " +
			"\"recur\": \"NO_RECUR,startDate=1349910000000,period=STOPS_ON_DATE,periodParam=1352674799999,\", " +
			"\"title\": \"a budget\", " +
			"\"expanded\": true, " +
			"\"currency\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyEAsSCEN1cnJlbmN5GOPTBgyiARJjb2xtZWRpc19nbWFpbC5jb20\", " +
			"\"amount\": 100.0, " +
			"\"projects\": \"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDwsSB1Byb2plY3QYlvYFDKIBEmNvbG1lZGlzX2dtYWlsLmNvbQ,\", " +
			"\"isCurrent\": false, " +
			"\"updated_on\": 1349967705, " +
			"\"includeCredit\": true, " +
			"\"categories\": \"crebits-aaa::agxzfmZsb3d6ci1ocmRyEAsSCENhdGVnb3J5GOuKIQyiAQtjcmViaXRzLWFhYQ," +
			"\"}";
	
	public Budget doBudgetFromJSON() {
		JSONObject jsonObj=null;
		try {			
				jsonObj = new JSONObject( new JSONTokener(JSNBUDGET));
		} catch (JSONException e) {
				fail("parse json failed");
		}	 
		return (Budget)flowzrSync.saveOrUpdateBudgetFromJSON(-1, jsonObj);	
	}

	public void testBudgetFromJSON() {
		budget1=doBudgetFromJSON();
		assertEquals(budget1.remoteKey,"privacy_gmail.com::agxzfmZsb3d6ci1ocmRyDgsSBkJ1ZGdldBj7mQcMogESY29sbWVkaXNfZ21haWwuY29t");
		assertEquals(budget1.startDate/1000,1349827200);
		assertEquals(budget1.endDate/1000,1352592000);	
		assertEquals(budget1.includeSubcategories,true);		
		assertEquals(budget1.recur,"NO_RECUR,startDate=1349910000000,period=STOPS_ON_DATE,periodParam=1352674799999,");	
		assertEquals(budget1.title,"a budget");	
		assertEquals(budget1.expanded,true);	
		assertEquals(budget1.currencyId,currency1.id);	
		assertEquals(budget1.amount,10000);
		assertEquals(budget1.projects,String.valueOf(project1.id));		
		assertEquals(budget1.isCurrent,false);
		assertEquals(budget1.includeCredit,true);
		assertEquals(budget1.categories,String.valueOf(category1.id));		
	}
	

}