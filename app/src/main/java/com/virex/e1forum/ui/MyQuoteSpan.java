package com.virex.e1forum.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.style.QuoteSpan;

import androidx.annotation.ColorInt;

public class MyQuoteSpan extends QuoteSpan {
    private int mStripWidth = 2;
    private int mGapWidth = 2;
    private final int mColor;
    public MyQuoteSpan() {
        super();
        mColor = 0xff0000ff;
    }
    public MyQuoteSpan(@ColorInt int color, int stripeWidth, int gapWidth) {
        super();
        mColor = color;
        mStripWidth = stripeWidth;
        mGapWidth = gapWidth;
    }
    public MyQuoteSpan(Parcel src) {
        mColor = src.readInt();
    }
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }
    /** @hide */
    public int getSpanTypeIdInternal() {
        //return TextUtils.QUOTE_SPAN;
        return 9;
    }
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel dest, int flags) {
        writeToParcelInternal(dest, flags);
    }
    /** @hide */
    public void writeToParcelInternal(Parcel dest, int flags) {
        dest.writeInt(mColor);
    }
    @ColorInt
    public int getColor() {
        return mColor;
    }
    public int getLeadingMargin(boolean first) {
        return mStripWidth + mGapWidth;
    }
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                                  int top, int baseline, int bottom,
                                  CharSequence text, int start, int end,
                                  boolean first, Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();
        p.setStyle(Paint.Style.FILL);
        p.setColor(mColor);
        c.drawRect(x, top, x + dir * mStripWidth, bottom, p);
        p.setStyle(style);
        p.setColor(color);
    }
}
