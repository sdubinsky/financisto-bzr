package ru.orangesoftware.financisto.adapter;

import java.util.Calendar;
import java.util.GregorianCalendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.MonthlyViewActivity;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.TransactionColumns;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.utils.Utils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class CreditCardStatementAdapter extends SimpleCursorAdapter implements Filterable {
 
	    private int layout;
	    
	    private Utils u;
	    private Currency currency;
	    
	    private final int scheduledStyle = Typeface.ITALIC;
	    private final int scheduledColor;
		private final int futureColor;
		private final int negativeColor;
	    private final int normalStyle = Typeface.NORMAL;
	    private final int normalColor = Color.LTGRAY;
	    
	    private boolean isStatementPreview = false;

		/**
	     * Create an adapter to display the expenses list of a credit card bill.
	     * @param context The context.
	     * @param layout The layout id.
	     * @param c The cursor with all the expenses of a given credit card bill.
	     * @param from Array of columns.
	     * @param to Id of the columns in layout.
	     * @param cur The credit card base currency.
	     */
	    public CreditCardStatementAdapter (Context context, int layout, Cursor c, String[] from, int[] to, Currency cur) {
	        super(context, layout, c, from, to);
	        this.layout = layout;
	        u = new Utils(context);
	        this.currency = cur;
	        futureColor = context.getResources().getColor(R.color.future_color);
	        scheduledColor = context.getResources().getColor(R.color.scheduled);
	        negativeColor = context.getResources().getColor(R.color.negative_amount);
	    }
	    
	    public boolean isStatementPreview() {
			return isStatementPreview;
		}

		public void setStatementPreview(boolean isStatementPreview) {
			this.isStatementPreview = isStatementPreview;
		}
	 
	    @Override
	    public View newView(Context context, Cursor cursor, ViewGroup parent) {
	 
	        Cursor c = getCursor();
	 
	        final LayoutInflater inflater = LayoutInflater.from(context);
	        View v = inflater.inflate(layout, parent, false);
	 
	        updateListItem(v, context, c);
	 
	        return v;
	    }
	 
	    @Override
	    public void bindView(View v, Context context, Cursor c) {
    		updateListItem(v, context, c);	        
	    }
	    
	    /**
	     * Update the list of expenses, displaying the elements of the Cursor. 
	     * @param v The view.
	     * @param context The context.
	     * @param c The cursor with all the bill transactions
	     */
	    private void updateListItem(View v, Context context, Cursor c) {
	    	// get amount of expense
	    	int valueCol = c.getColumnIndex(TransactionColumns.FROM_AMOUNT);
	    	long value = c.getLong(valueCol);
	    	// is scheduled?
	    	boolean isScheduled = c.getInt(c.getColumnIndex(TransactionColumns.IS_TEMPLATE))==2;
	        
	        // get columns values or needed parameters
	        long date = c.getLong(TransactionColumns.Indicies.DATETIME);
	        String note = c.getString(TransactionColumns.Indicies.NOTE);
	        int locId = c.getInt(TransactionColumns.Indicies.LOCATION_ID);
	        String location = null;
	        String desc = "";
	        boolean future = date>Calendar.getInstance().getTimeInMillis();
	        
	        // draw headers for payments, various credits and expenses sections
	        if (c.getColumnIndex(MonthlyViewActivity.HEADER_PAYMENTS) != -1) {
        		drawGroupTitle(context.getResources().getString(R.string.header_payments), v);   	
	        	return;
	        } else if (c.getColumnIndex(MonthlyViewActivity.HEADER_CREDITS) != -1) {
	        	drawGroupTitle(context.getResources().getString(R.string.header_credits), v); 
	        	return;
	        } else if (c.getColumnIndex(MonthlyViewActivity.HEADER_EXPENSES) != -1) {
	        	drawGroupTitle(context.getResources().getString(R.string.header_expenses), v); 
	        	return;
	        } 
	        
	        /* 
	         * Set description: 
	         * a) if location is set, format description considering location
	         *    - "Location (Note)"
	         * b) otherwise, show description as note
	         *    - "Note" 
	         */
	        if (locId>0) { 
	        	DatabaseAdapter dba = new DatabaseAdapter(context);
	        	dba.open();
	        	location = dba.getLocation(locId).name;
	        	dba.close();
	        	if (note!=null && note.length()>0) {
	        		desc = location+" ("+note+")";
	        	} else {
	        		desc = location;
	        	}
	        } else {
	        	desc = note;
	        }
	 
	        // set expenses date, description and value to the respective columns
	        TextView dateText = (TextView) v.findViewById(R.id.list_date);
	        TextView descText = (TextView) v.findViewById(R.id.list_note);
	        TextView valueText = (TextView) v.findViewById(R.id.list_value);
	
	        dateText.setBackgroundColor(Color.rgb(17,17,17));
        	descText.setBackgroundColor(Color.rgb(17,17,17));
        	valueText.setBackgroundColor(Color.rgb(17,17,17));
        	
	        if (dateText != null) {
	            dateText.setText(getDate(date)+" ");
	        }
	        if (descText != null) {
	            descText.setText(desc);
	        }
	        if (valueText != null) {
	        	if (isStatementPreview) {
	        		u.setAmountText(valueText, currency, (-1)*value, false);
	        	} else {
	        		u.setAmountText(valueText, currency, value, false);
	        	}
	        }
	        
	        // set style 
	        if (isScheduled) {
	        	dateText.setTypeface(Typeface.defaultFromStyle(scheduledStyle), scheduledStyle);
	        	descText.setTypeface(Typeface.defaultFromStyle(scheduledStyle), scheduledStyle);
	        	valueText.setTypeface(Typeface.defaultFromStyle(scheduledStyle), scheduledStyle);
	        	dateText.setTextColor(scheduledColor);
	        	descText.setTextColor(scheduledColor);
	        	valueText.setTextColor(scheduledColor);
	        } else {
	        	dateText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);
	        	descText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);
	        	valueText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);
	        	
	        	// set color
				if (future) {
					// future expenses
					dateText.setTextColor(futureColor);
		        	descText.setTextColor(futureColor);
		        	valueText.setTextColor(futureColor);	
				} else {
					// normal
					dateText.setTextColor(normalColor);
		        	descText.setTextColor(normalColor);
		        	// display colored negative values in month preview, but not in bill preview 
		        	if (value<0 && !isStatementPreview) valueText.setTextColor(negativeColor); else valueText.setTextColor(normalColor);
				}       	
	        }
	    }
	    
	    private void drawGroupTitle(String title, View v) {
	    	TextView dateText = (TextView) v.findViewById(R.id.list_date);
	        TextView descText = (TextView) v.findViewById(R.id.list_note);
	        TextView valueText = (TextView) v.findViewById(R.id.list_value);
        	dateText.setText("");
	        descText.setText(title);
	        valueText.setText("");
	        dateText.setBackgroundColor(Color.DKGRAY);
        	descText.setBackgroundColor(Color.DKGRAY);
        	valueText.setBackgroundColor(Color.DKGRAY);
        	descText.setTypeface(Typeface.defaultFromStyle(normalStyle), normalStyle);
        	descText.setTextColor(normalColor);
	    }
	    
	    /**
	     * Return the string for date in the following format: dd/MM/yy.
	     * @param date Time in milliseconds.
	     * @return The string representing the given time in the format dd/MM/yy.
	     */
	    private String getDate(long date) {
	    	Calendar cal = new GregorianCalendar();
        	cal.setTimeInMillis(date);
        	int d = cal.get(Calendar.DAY_OF_MONTH);
        	int m = cal.get(Calendar.MONTH)+1;
        	int y = cal.get(Calendar.YEAR);
        	return (d<10?"0"+d:d)+"/"+(m<10?"0"+m:m)+"/"+(y-2000);
	    }
	}
