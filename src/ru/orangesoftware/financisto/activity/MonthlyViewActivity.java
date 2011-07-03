package ru.orangesoftware.financisto.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.CreditCardStatementAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.BlotterColumns;
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.AccountType;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.Utils;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
	private Calendar closingDate;
	
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
	
	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
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
		
        if (month==0 && year==0) {
	        // get current month and year in first launch
			Calendar cal = Calendar.getInstance();
			month = cal.get(Calendar.MONTH) + 1;
			year = cal.get(Calendar.YEAR);
        }
		
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
					title = (account.title==null|| account.title.length()==0? account.cardIssuer: account.title);
					paymentDay = 1;
					closingDay = 31;
					setTitle();
					setInterval();
					
					// set payment date and label on total bar
					TextView totalLabel = (TextView) findViewById(R.id.monthly_result_label);
					totalLabel.setText(getResources().getString(R.string.monthly_result));
				}
			} else {
				if (account.title==null|| account.title.length()==0) {
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
		
		this.closingDate = new GregorianCalendar(close.get(Calendar.YEAR), 
				  								 close.get(Calendar.MONTH),
				  								 close.get(Calendar.DAY_OF_MONTH));
		
		// Verify custom closing date
		int periodKey = Integer.parseInt(Integer.toString(close.get(Calendar.MONTH))+
					 	Integer.toString(close.get(Calendar.YEAR)));
		
		int cd = dbAdapter.getCustomClosingDay(accountId, periodKey);
		if (cd>0) {
			// use custom closing day
			close.set(Calendar.DAY_OF_MONTH, cd);
		}
		
		// Verify custom opening date = closing day of previous month + 1
		periodKey = Integer.parseInt(Integer.toString(open.get(Calendar.MONTH))+
				 	Integer.toString(open.get(Calendar.YEAR)));
		
		int od = dbAdapter.getCustomClosingDay(accountId, periodKey);
		if (od>0) {
			// use custom closing day
			open.set(Calendar.DAY_OF_MONTH, od);
			open.add(Calendar.DAY_OF_MONTH, +1);
		}
		
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
			Cursor expenses = dbAdapter.getAllExpenses(accountId, open.getTimeInMillis(), close.getTimeInMillis());
			Cursor credits = dbAdapter.getCredits(accountId, open.getTimeInMillis(), close.getTimeInMillis());
			Cursor payments = dbAdapter.getPayments(accountId, open.getTimeInMillis(), close.getTimeInMillis());
			
			transactionsCursor = new MergeCursor(new Cursor[] { getHeader(HEADER_PAYMENTS, payments.getCount()), payments, 
																getHeader(HEADER_CREDITS, credits.getCount()), credits, 
																getHeader(HEADER_EXPENSES, expenses.getCount()), expenses});
			
		} else {
			// account filtering: credit card transactions, from open to close date
			transactionsCursor = dbAdapter.getAllTransactions(accountId, open.getTimeInMillis(), close.getTimeInMillis());
		}

    	TextView totalText = (TextView)findViewById(R.id.monthly_result);
    	
    	if (transactionsCursor == null || transactionsCursor.isClosed() || transactionsCursor.getCount()==0) {
    		// display total = 0
    		u.setAmountText(totalText, currency, 0, false);
    		totalText.setTextColor(Color.BLACK);
    		totalText.setVisibility(View.VISIBLE);
    		
    		// hide list and display empty message
    		this.getListView().setVisibility(View.GONE);
    		setListAdapter(null);
    		findViewById(android.R.id.empty).setVisibility(View.VISIBLE);

    	} else { // display data
     		startManagingCursor(transactionsCursor);
    		
    		// Mapping data from database
    		String[] from = new String[] {BlotterColumns.datetime.name(), BlotterColumns.note.name(), BlotterColumns.from_amount.name()};
    		int[] to = new int[] {R.id.list_date, R.id.list_note, R.id.list_value};
    		
    		// Mapping data to view
    		CreditCardStatementAdapter expenses = new CreditCardStatementAdapter(dbAdapter, this, R.layout.credit_card_transaction, transactionsCursor, from, to, currency, accountId);
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
    		findViewById(android.R.id.empty).setVisibility(View.GONE);
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
		int fromAccountIdCol = cursor.getColumnIndex(BlotterColumns.from_account_id.name());
		int toAccountIdCol = cursor.getColumnIndex(BlotterColumns.to_account_id.name());
		
		if (isStatementPreview) {
			// exclude payments
			for (int i=0; i<cursor.getCount(); i++) {
				if (cursor.getInt(cursor.getColumnIndex(BlotterColumns.is_ccard_payment.name()))==0) {
					if (cursor.getInt(cursor.getColumnIndex(BlotterColumns.is_ccard_payment.name()))==0) {
						if (cursor.getLong(fromAccountIdCol)==accountId) {
							total += cursor.getLong(cursor.getColumnIndex(BlotterColumns.from_amount.name()));	
						} else if (cursor.getLong(toAccountIdCol)==accountId) {
							total += cursor.getLong(cursor.getColumnIndex(BlotterColumns.to_amount.name()));	
						}
					}
				}
				cursor.moveToNext();
			}
		} else {
			// consider all transactions
			for (int i=0; i<cursor.getCount(); i++) {
				if (cursor.getLong(fromAccountIdCol)==accountId) {
					total += cursor.getLong(cursor.getColumnIndex(BlotterColumns.from_amount.name()));	
				} else if (cursor.getLong(toAccountIdCol)==accountId) {
					total += cursor.getLong(cursor.getColumnIndex(BlotterColumns.to_amount.name()));	
				}
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
		switch (resultCode) {
		case RESULT_OK:
			int update = data.getIntExtra(CCardStatementClosingDayActivity.UPDATE_VIEW, 0);
			if (update>0) {
				setCCardTitle();
				setCCardInterval();
			}
			break;
		case RESULT_CANCELED:
			break;
		}
	}
	
	private Cursor getHeader(String type, int count) {
        String[] headerColumns = new String[BlotterColumns.NORMAL_PROJECTION.length+1];
        System.arraycopy(BlotterColumns.NORMAL_PROJECTION, 0, headerColumns, 0, BlotterColumns.NORMAL_PROJECTION.length);
        headerColumns[headerColumns.length-1] = type;
		MatrixCursor header = new MatrixCursor(headerColumns);
		if (count > 0) {
            Object[] columns = new Object[headerColumns.length];
            columns[columns.length-1] = count;
			header.addRow(columns);
		}
		return header;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Allow changes on credit card closing date (for statements preview only)
		if (isStatementPreview) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.statement_preview_menu, menu);
			return true;
		} else {
			return super.onCreateOptionsMenu(menu);
		}
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		Intent intent = new Intent(this, CCardStatementClosingDayActivity.class);
		
		int closingDay = getClosingDate(month, year).get(Calendar.DAY_OF_MONTH);
		
		switch (item.getItemId()) {
			case R.id.opt_menu_closing_day:
				// call credit card closing day sending period
	    		intent.putExtra(CCardStatementClosingDayActivity.PERIOD_MONTH, closingDate.get(Calendar.MONTH));
	    		intent.putExtra(CCardStatementClosingDayActivity.PERIOD_YEAR, closingDate.get(Calendar.YEAR));
	    		intent.putExtra(CCardStatementClosingDayActivity.ACCOUNT, accountId);
	    		intent.putExtra(CCardStatementClosingDayActivity.REGULAR_CLOSING_DAY, closingDay);
	    		startActivityForResult(intent, 16);
	            return true;
	            
			default:
	            return super.onOptionsItemSelected(item);
		}
    }
}
