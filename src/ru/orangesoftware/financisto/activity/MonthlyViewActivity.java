package ru.orangesoftware.financisto.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.CreditCardStatementAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.Utils;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Display the credit card bill, including scheduled and future transactions for a given period.
 * Display only expenses, ignoring payments (positive values) in Credit Card accounts. 
 * @author Abdsandryk
 */
public class MonthlyViewActivity extends ListActivity {
	
	private DatabaseAdapter dbAdapter;
	private Cursor transactionsCursor;
	
	private long accountId = 0;
	private Account account;
	private Currency currency;
	private boolean isCreditCard = false;
	private boolean isStatementPreview = false;
	
	private String title;
	private int closingDay = 0;
	private int paymentDay = 0;
	private String paymentDayStr;
	
	private int month = 0;
	private String monthStr;
	private int year = 0;
	private String yearStr;
	
	private ImageButton bPrevious;
	private ImageButton bNext;
	
	private int negativeColor;
	private int positiveColor;
	
	private Utils u;

	public static final String ACCOUNT_EXTRA = "account_id";
	public static final String BILL_PREVIEW_EXTRA = "bill_preview";
	
	public static final String HEADER_PAYMENTS = "bill_payments";
	public static final String HEADER_CREDITS  = "bill_credits";
	public static final String HEADER_EXPENSES = "bill_expenses";
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monthly_view);
        
        u = new Utils(this);
        negativeColor = this.getResources().getColor(R.color.negative_amount);
        positiveColor = this.getResources().getColor(R.color.positive_amount);
        
        Intent intent = getIntent();
		if (intent != null) {
			accountId = intent.getLongExtra(ACCOUNT_EXTRA, 0);
			isStatementPreview = intent.getBooleanExtra(BILL_PREVIEW_EXTRA, false);
		}		
		
		initialize();
    }
    
    /**
     * When activity lifecycle ends, release resources
     */
    @Override
    public void onDestroy() {
    	// close cursor
    	if (transactionsCursor != null) {
    		transactionsCursor.close();
    	}
    	dbAdapter.close();
    	super.onDestroy();
    }

    
    
    /**
     * Initialize data and GUI elements.
     */
    private void initialize() {
    	
    	// get account data
		dbAdapter = new DatabaseAdapter(this);
		dbAdapter.open();
		
		// set currency based on account
		MyEntityManager em = dbAdapter.em();
		account = em.getAccount(accountId);
		
        // get current month and year
		Calendar cal = Calendar.getInstance();
		month = cal.get(Calendar.MONTH) + 1;
		year = cal.get(Calendar.YEAR);
		
		// set part of the title, based on account name: "<CCARD> Bill"
		if (account != null) {
			
			// get account type
			isCreditCard = AccountType.valueOf(account.type).isCreditCard;
	
			currency = account.currency;
			
			if (isCreditCard) {
				if (isStatementPreview) { 
					// assuming that expensesOnly is true only if payment and closing days > 0 [BlotterActivity]
					title = getString(R.string.ccard_statement_title);
					String accountTitle = account.title;
					if (account.title==null || account.title.length()==0) {
						accountTitle = account.cardIssuer;
					}
					String toReplace = getString(R.string.ccard_par);
					title = title.replaceAll(toReplace, accountTitle);
					paymentDay = account.paymentDay;
					closingDay = account.closingDay;
					// set activity window title
					this.setTitle(R.string.ccard_statement);
					setCCardTitle();
					setCCardInterval();
				} else {
					title = (account.title==null||account.title.length()==0?account.cardIssuer:account.title);
					paymentDay = 1;
					closingDay = 31;
					setTitle();
					setInterval();
					
					// set payment date and label on total bar
					TextView totalLabel = (TextView) findViewById(R.id.monthly_result_label);
					totalLabel.setText(getResources().getString(R.string.monthly_result));
				}
			} else {
				if (account.title==null||account.title.length()==0) {
					if (isCreditCard) {
						// title = <CARD_ISSUER>
						title = account.cardIssuer;
					} else {
						// title = <ACCOUNT_TYPE_TITLE>
						AccountType type = AccountType.valueOf(account.type);
						title = getString(type.titleId);
					}
				} else {
					// title = <TITLE>
					title = account.title;
				}
				
				paymentDay = 1;
				closingDay = 31;
				setTitle();
				setInterval();
				
				// set payment date and label on total bar
				TextView totalLabel = (TextView) findViewById(R.id.monthly_result_label);
				totalLabel.setText(getResources().getString(R.string.monthly_result));
			}		
		
			bPrevious = (ImageButton) findViewById(R.id.bt_month_previous);
			bPrevious.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					month--;
					if (month<1) {
						month=12;
						year--;
					}
					if (isCreditCard) {
						if (isStatementPreview) {
							setCCardTitle();
							setCCardInterval();
						} else {
							setTitle();
							setInterval();
						}
					} else {
						setTitle();
						setInterval();
					}
				}			
			});
			
			bNext = (ImageButton) findViewById(R.id.bt_month_next);
			bNext.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					month++;
					if (month>12) {
						month=1;
						year++;
					}
					if (isCreditCard) {
						if (isStatementPreview) {
							setCCardTitle();
							setCCardInterval();
						} else {
							setTitle();
							setInterval();
						}
					} else {
						setTitle();
						setInterval();
					}
				}			
			});
		
		}
	}

    /**
     * Configure the interval based on the bill period of a credit card.
     * Attention: 
     *   Calendar.MONTH = 0 to 11
     *   integer  month = 1 to 12
     */
	private void setCCardInterval() {
		
		Calendar close = getClosingDate(month, year);
		Calendar open;
		
		if (month>1) {
			// closing date from previous month
			open = getClosingDate(month-1, year);
		} else {
			open = getClosingDate(12, year-1);
		}
		// add one day to the closing date of previous month
		open.add(Calendar.DAY_OF_MONTH, +1);
		
		// adjust time for closing day
		close.set(Calendar.HOUR_OF_DAY, 23);
		close.set(Calendar.MINUTE, 59);
		close.set(Calendar.SECOND, 59);
		
		fillData(open, close);
	}
	
    /**
     * Configure the interval in a monthly perspective.
     * Attention: 
     *   Calendar.MONTH = 0 to 11
     *   integer  month = 1 to 12
     */
	private void setInterval() {
		
		Calendar close = new GregorianCalendar(year, month-1, getLastDayOfMonth(month, year));
		Calendar open = new GregorianCalendar(year, month-1, 1);
		
		// adjust time for closing day
		close.set(Calendar.HOUR_OF_DAY, 23);
		close.set(Calendar.MINUTE, 59);
		close.set(Calendar.SECOND, 59);
		
		fillData(open, close);
	}
	
	/**
	 * Returns the day on which the credit card bill closes for a given month/year.
	 * @param month
	 * @param year
	 * @return
	 */
	private Calendar getClosingDate(int month, int year) {
		int m = month;
		if (closingDay > paymentDay) {
			m--;
		}
		int maxDay = getLastDayOfMonth(m, year);
		int day = closingDay;
		if (closingDay>maxDay) {
			day = maxDay;
		}
		
		return new GregorianCalendar(year, m-1, day);
	}
	
	/**
	 * Return the last day (maximum value of day) of the given month.
	 * @param month
	 * @param year
	 * @return
	 */
	private int getLastDayOfMonth(int month, int year) {
		Calendar calCurr = GregorianCalendar.getInstance();
		calCurr.set(year, month-1, 1); // Months are 0 to 11
		return calCurr.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
	}


	/**
	 * Get data for a given period and display the related credit card expenses. 
	 * @param open Start of period.
	 * @param close End of period.
	 */
	private void fillData(Calendar open, Calendar close) {
		// closing cursor from previous request
    	if (transactionsCursor != null) {
    		transactionsCursor.close();
    	}
		
		if (isStatementPreview) {
			// display expenses and credits separated	    	
			Cursor expenses = dbAdapter.getAllExpenses(String.valueOf(accountId), 
    													  String.valueOf(open.getTimeInMillis()), 
    													  String.valueOf(close.getTimeInMillis()));
			Cursor credits = dbAdapter.getCredits(String.valueOf(accountId), 
					  									String.valueOf(open.getTimeInMillis()), 
					  									String.valueOf(close.getTimeInMillis()));
			Cursor payments = dbAdapter.getPayments(String.valueOf(accountId), 
														String.valueOf(open.getTimeInMillis()), 
														String.valueOf(close.getTimeInMillis()));
			transactionsCursor = new MergeCursor(new Cursor[] { getHeader(HEADER_PAYMENTS, payments.getCount()), payments, 
																getHeader(HEADER_CREDITS, credits.getCount()), credits, 
																getHeader(HEADER_EXPENSES, expenses.getCount()), expenses});
			
		} else {
			// account filtering: credit card transactions, from open to close date
			transactionsCursor = dbAdapter.getAllTransactions(String.valueOf(accountId), 
    													  String.valueOf(open.getTimeInMillis()), 
    													  String.valueOf(close.getTimeInMillis()));
		}
    	
    	TextView totalText = (TextView)findViewById(R.id.monthly_result);
    	
    	if (transactionsCursor == null || transactionsCursor.getCount()==0) {
    		// display total = 0
    		u.setAmountText(totalText, currency, 0, false);
    		totalText.setTextColor(Color.BLACK);
    		totalText.setVisibility(View.VISIBLE);
    		
    		// hide list and display empty message
    		this.getListView().setVisibility(View.GONE);
    		((TextView)findViewById(android.R.id.empty)).setVisibility(View.VISIBLE);

    	} else { // display data
     		startManagingCursor(transactionsCursor);
    		
    		// Mapping data from database
    		String[] from = new String[] {TransactionColumns.DATETIME, TransactionColumns.NOTE, TransactionColumns.FROM_AMOUNT};
    		int[] to = new int[] {R.id.list_date, R.id.list_note, R.id.list_value};
    		
    		// Mapping data to view
    		CreditCardStatementAdapter expenses = new CreditCardStatementAdapter(dbAdapter, this, R.layout.credit_card_transaction, transactionsCursor, from, to, currency);
    		expenses.setStatementPreview(isStatementPreview);
    		setListAdapter(expenses);
    		
    		// calculate total
    		long total = calculateTotal(expenses.getCursor());
    		// display total
    		if (isStatementPreview) {
    			u.setAmountText(totalText, currency, (-1)*total, false);
    			totalText.setTextColor(Color.BLACK);
    		} else {
	    		if (total<0) { 
	    			u.setAmountText(totalText, currency, (-1)*total, false);
	    			totalText.setTextColor(negativeColor);
	    		} else {
	    			u.setAmountText(totalText, currency, total, false);
	    			totalText.setTextColor(positiveColor);
	    		}
    		}
    		totalText.setVisibility(View.VISIBLE);
    		
    		// display list and hide empty message
    		this.getListView().setVisibility(View.VISIBLE);
    		((TextView)findViewById(android.R.id.empty)).setVisibility(View.GONE);
    	}
    }
	
	/**
	 * Calculate the total amount based on cursor expenses and various credits.
	 * Credit card payments are not included.
	 * @param cursor
	 * @return The total amount.
	 */
	private long calculateTotal(Cursor cursor) {
		long total = 0;
		cursor.moveToFirst();
		if (isStatementPreview) {
			// exclude payments
			for (int i=0; i<cursor.getCount(); i++) {
				if (cursor.getInt(cursor.getColumnIndex(TransactionColumns.IS_CCARD_PAYMENT))==0) {
					total += cursor.getLong(cursor.getColumnIndex(TransactionColumns.FROM_AMOUNT));
				}
				cursor.moveToNext();
			}
		} else {
			// consider all transactions
			for (int i=0; i<cursor.getCount(); i++) {
				total += cursor.getLong(cursor.getColumnIndex(TransactionColumns.FROM_AMOUNT));
				cursor.moveToNext();
			}
		}
		
		return total;		
	}


	/**
	 * Adjust the title based on the credit card's payment day.
	 */
	private void setCCardTitle() {
		
		Calendar date = new GregorianCalendar(year, month-1, paymentDay);
		
		monthStr = Integer.toString(date.get(Calendar.MONTH)+1);
		yearStr = Integer.toString(date.get(Calendar.YEAR));
		
		if (paymentDay<10) {
        	paymentDayStr = "0"+paymentDay;
        } else {
        	paymentDayStr = Integer.toString(paymentDay);
        }
		
		if (monthStr.length()<2) {
			monthStr = "0"+monthStr;
		}
		
		String pd = paymentDayStr + "/" + monthStr + "/" + yearStr;
		
        // set payment date and label on title bar
		TextView label = (TextView)findViewById(R.id.monthly_view_title);
		label.setText(title+"\n" + pd);
		// set payment date and label on total bar
		TextView totalLabel = (TextView) findViewById(R.id.monthly_result_label);
		totalLabel.setText(getResources().getString(R.string.bill_on)+" "+pd);
	}
	
	/**
	 * Adjust the title based on the credit card's payment day.
	 */
	private void setTitle() {
		
		Calendar date = new GregorianCalendar(year, month-1, 1);
		 
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMM yyyy");  
		String pd = dateFormat.format(date.getTime()); 
		
		TextView label = (TextView)findViewById(R.id.monthly_view_title);
		label.setText(title+"\n" + pd);
		
	}

	// Update view 
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
		setCCardInterval();
	}
	
	private Cursor getHeader(String type, int count) {
		MatrixCursor header = new MatrixCursor(new String[] {
				TransactionColumns.ID, 
				TransactionColumns.FROM_ACCOUNT_ID, 
				TransactionColumns.TO_ACCOUNT_ID,
				TransactionColumns.CATEGORY_ID, 
				TransactionColumns.PROJECT_ID, 
				TransactionColumns.NOTE, 
				TransactionColumns.FROM_AMOUNT, 
				TransactionColumns.TO_AMOUNT,
				TransactionColumns.DATETIME,
				TransactionColumns.LOCATION_ID,
				TransactionColumns.PROVIDER, 
				TransactionColumns.ACCURACY, 
				TransactionColumns.LATITUDE, 
				TransactionColumns.LONGITUDE,
				TransactionColumns.IS_TEMPLATE,
				TransactionColumns.TEMPLATE_NAME,
				TransactionColumns.RECURRENCE,
				TransactionColumns.NOTIFICATION_OPTIONS,
				TransactionColumns.STATUS,
				TransactionColumns.ATTACHED_PICTURE,
				TransactionColumns.IS_CCARD_PAYMENT, type});
		if (count>0) {
			header.addRow(new Object[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,count});
		}
		return header;
	}
	
	
}
