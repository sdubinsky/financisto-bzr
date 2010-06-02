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
package ru.orangesoftware.financisto.graph;

import ru.orangesoftware.financisto.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.view.View;

public class GraphWidget extends View {

	public static final Paint NAME_PAINT = new Paint();
	public static final Paint AMOUNT_PAINT = new Paint();
	public static final Paint LINE_PAINT = new Paint();
	public static final int NAME_HEIGHT;
	
	static {
		Rect rect = new Rect();
		NAME_PAINT.setColor(Color.WHITE);
		NAME_PAINT.setAntiAlias(true);
		NAME_PAINT.setTextAlign(Align.LEFT);
		NAME_PAINT.setTextSize(14);
		NAME_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
		NAME_PAINT.getTextBounds("AAA", 0, 3, rect);		
		NAME_HEIGHT = rect.height();
		AMOUNT_PAINT.setColor(Color.WHITE);
		AMOUNT_PAINT.setAntiAlias(true);
		AMOUNT_PAINT.setTextSize(12);
		AMOUNT_PAINT.setTextAlign(Align.CENTER);
		LINE_PAINT.setStyle(Style.FILL);		
	}

	private static final int zeroColor = Resources.getSystem().getColor(android.R.color.secondary_text_dark);
	private static final int zeroLineColor = zeroColor;

	private static final int DY = 6;
	private static final int LINE_HEIGHT = 30;
	
	private final int positiveColor;
	private final int negativeColor;	
	private final int positiveLineColor = Color.argb(255, 124, 198, 35);
	private final int negativeLineColor = Color.argb(255, 239, 156, 0);	
	
	private final GraphUnit unit;

	private final long maxAmount;
	private final long maxAmountWidth;

	public GraphWidget(Context context, GraphUnit unit, long maxAmount, long maxAmountWidth) {
		super(context);		
		Resources r = context.getResources();
		positiveColor = r.getColor(R.color.positive_amount);
		negativeColor = r.getColor(R.color.negative_amount);		
		this.unit = unit;
		this.maxAmount = maxAmount;
		this.maxAmountWidth = maxAmountWidth;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int x = getPaddingLeft();
		int y = getPaddingTop();
		int w = getWidth()-getPaddingLeft()-getPaddingRight();
		GraphUnit u = this.unit;
		String name = u.name;
		canvas.drawText(name, x, y+NAME_HEIGHT, NAME_PAINT);
		y += NAME_HEIGHT+DY;
		for (Amount a : u.amounts.values()) {
			long amount = a.getAmount();				
			int lineWidth = Math.max(1, (int)(1.0*Math.abs(amount)/maxAmount*(w-DY-maxAmountWidth)));
			LINE_PAINT.setColor(amount == 0 ? zeroLineColor : (amount > 0 ? positiveLineColor : negativeLineColor));
			canvas.drawRect(x, y, x+lineWidth, y+LINE_HEIGHT, LINE_PAINT);
			AMOUNT_PAINT.setColor(amount == 0 ? zeroColor : (amount > 0 ? positiveColor : negativeColor));
			canvas.drawText(a.getAmountText(), 
					x+lineWidth+DY+a.amountTextWidth/2, 
					y+LINE_HEIGHT/2+2, 
					AMOUNT_PAINT);
			y += LINE_HEIGHT+DY;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int specWidth = MeasureSpec.getSize(widthMeasureSpec);
		int h = 0;
		h += NAME_HEIGHT + DY;
		h += (LINE_HEIGHT+DY)*unit.amounts.size();
		setMeasuredDimension(specWidth, getPaddingTop()+h+getPaddingBottom());
	}

}
