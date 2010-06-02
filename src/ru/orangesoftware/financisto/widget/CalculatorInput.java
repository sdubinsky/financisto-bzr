/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.widget;

import java.math.BigDecimal;
import java.util.Stack;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.Utils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CalculatorInput extends Activity implements OnClickListener {

	public static final int[] buttons = { R.id.b0, R.id.b1, R.id.b2, R.id.b3,
			R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.bAdd,
			R.id.bSubtract, R.id.bDivide, R.id.bMultiply, R.id.bPercent,
			R.id.bPlusMinus, R.id.bDot, R.id.bResult, R.id.bClear, R.id.bDelete };

	private TextView tvResult;

	private final Stack<String> stack = new Stack<String>();
	private String result = "0";
	private boolean isRestart = true;
	private boolean isInEquals = false;
	private char lastOp = '\0';

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.calculator);
		//getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		//((TextView)findViewById(android.R.id.title)).setText(R.string.calculator);

		for (int id : buttons) {
			Button b = (Button) findViewById(id);
			b.setOnClickListener(this);
		}

		tvResult = (TextView) findViewById(R.id.result);
		
		Button b = (Button)findViewById(R.id.bOK);
		b.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				close();
			}		
		});
		b = (Button)findViewById(R.id.bCancel);
		b.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}		
		});
		
		Intent intent = getIntent();
		if (intent != null) {
			String amount = intent.getStringExtra(AmountInput.EXTRA_AMOUNT);
			if (amount != null) {
				setDisplay(amount);
			}
		}
		
	}

	@Override
	public void onClick(View v) {
		Button b = (Button) v;
		char c = b.getText().charAt(0);
		onButtonClick(c);
	}

	private void setDisplay(String s) {
		result = s;
		tvResult.setText(s);
	}

	private void onButtonClick(char c) {
		switch (c) {
		case 'C':
			resetAll();
			break;
		case '<':
			doBackspace();
			break;
		default:
			doButton(c);
			break;
		}
	}

	private void resetAll() {
		setDisplay("0");
		lastOp = '\0';
		isRestart = true;
		stack.clear();
	}

	private void doBackspace() {
		String s = tvResult.getText().toString();
		if (s == "0" || isRestart) {
			return;
		}
		String newDisplay = s.length() > 1 ? s.substring(0, s.length() - 1) : "0";
		if ("-".equals(newDisplay)) {
			newDisplay = "0";
		}
		setDisplay(newDisplay);
	}

	private void doButton(char c) {
		if (Character.isDigit(c) || c == '.') {
			addChar(c);
		} else {
			switch (c) {
				case '+':
				case '-':
				case '/':
				case '*':
					doOpChar(c);
					break;
				case '%':
					doPercentChar();
					break;
				case '=':
				case '\r':
					doEqualsChar();
					break;
				case '±':
					setDisplay(new BigDecimal(result).negate().toPlainString());
					break;
			}
		}
	}

	private void addChar(char c) {
		String s = tvResult.getText().toString();
		if (c == '.' && s.indexOf('.') != -1 && !isRestart) {
			return;
		}
		if (s == "0") {
			s = String.valueOf(c);
		} else {
			s += c;
		}
		setDisplay(s);
		if (isRestart) {
			setDisplay(String.valueOf(c));
			isRestart = false;
		}
	}

	private void doOpChar(char op) {
		if (isInEquals) {
			stack.clear();
			isInEquals = false;
		}
		stack.push(result);
		doLastOp();
		lastOp = op;
	}

	private void doLastOp() {
		isRestart = true;
        if (lastOp == '\0' || stack.size() == 1)
        {
            return;
        }

        String valTwo = stack.pop();
		String valOne = stack.pop();
		switch (lastOp)
		{
			case '+':
				stack.push(new BigDecimal(valOne).add(new BigDecimal(valTwo)).toPlainString());
				break;
			case '-':
				stack.push(new BigDecimal(valOne).subtract(new BigDecimal(valTwo)).toPlainString());
				break;
			case '*':
				stack.push(new BigDecimal(valOne).multiply(new BigDecimal(valTwo)).toPlainString());
				break;
			case '/':
				stack.push(new BigDecimal(valOne).divide(new BigDecimal(valTwo), 2, BigDecimal.ROUND_HALF_UP).toPlainString());
				break;
			default:
				break;
		}
		setDisplay(stack.peek());
        if (isInEquals)
        {
            stack.push(valTwo);
        }
	}

	private void doPercentChar() {
		if (stack.size() == 0)
			return;
		setDisplay(new BigDecimal(result).divide(Utils.HUNDRED).multiply(new BigDecimal(stack.peek())).toPlainString());
	}

	private void doEqualsChar() {
		if (lastOp == '\0') {
			return;
		}
		if (!isInEquals) {
			isInEquals = true;
			stack.push(result);
		}
		doLastOp();
	}

	private void close() {
		Intent data = new Intent();		
		data.putExtra(AmountInput.EXTRA_AMOUNT, result);
		setResult(RESULT_OK, data);
		finish();
	}
}
