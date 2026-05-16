package com.example.spottermobile.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * A lightweight bar chart View that draws a 7-day booking trend entirely
 * on Canvas — no external library required.
 *
 * Usage:
 *   1. Add <com.example.spottermobile.views.BookingsBarChartView> to XML.
 *   2. Call setData(labels, values) from your Activity after loading DB data.
 */
public class BookingsBarChartView extends View {

    // ── Paints ─────────────────────────────────────────────────────────────────

    private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Data ───────────────────────────────────────────────────────────────────

    private String[] labels;   // day labels e.g. "Mon\n05/12"
    private int[]    values;   // booking counts per day
    private int      maxValue; // max for scaling

    // ── Dimensions ─────────────────────────────────────────────────────────────

    private static final float LABEL_HEIGHT_DP  = 32f;
    private static final float VALUE_HEIGHT_DP  = 20f;
    private static final float BOTTOM_PADDING   = 8f;
    private static final float BAR_RADIUS_DP    = 6f;
    private static final float LEFT_PADDING_DP  = 8f;
    private static final float RIGHT_PADDING_DP = 8f;

    // ── Constructor ────────────────────────────────────────────────────────────

    public BookingsBarChartView(Context context) {
        super(context);
        init();
    }

    public BookingsBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BookingsBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        barPaint.setColor(Color.parseColor("#3B82F6"));
        barPaint.setStyle(Paint.Style.FILL);

        todayPaint.setColor(Color.parseColor("#1E3A8A"));
        todayPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(Color.parseColor("#64748B"));
        labelPaint.setTextSize(10 * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        valuePaint.setColor(Color.parseColor("#1E3A8A"));
        valuePaint.setTextSize(11 * density);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        gridPaint.setColor(Color.parseColor("#E2E8F0"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        axisPaint.setColor(Color.parseColor("#CBD5E1"));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(1.5f * density);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * @param labels  Day labels — use "\n" to split into two lines (e.g. "Mon\n05/12")
     * @param values  Booking counts aligned to labels
     */
    public void setData(String[] labels, int[] values) {
        this.labels = labels;
        this.values = values;
        this.maxValue = 0;
        for (int v : values) if (v > maxValue) maxValue = v;
        if (maxValue == 0) maxValue = 1; // avoid division by zero
        invalidate();
    }

    // ── Drawing ────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (labels == null || values == null || labels.length == 0) return;

        float density = getResources().getDisplayMetrics().density;
        int   w       = getWidth();
        int   h       = getHeight();
        int   n       = labels.length;

        float leftPad   = LEFT_PADDING_DP  * density;
        float rightPad  = RIGHT_PADDING_DP * density;
        float labelH    = LABEL_HEIGHT_DP  * density;
        float valueH    = VALUE_HEIGHT_DP  * density;
        float botPad    = BOTTOM_PADDING   * density;
        float barRadius = BAR_RADIUS_DP    * density;

        float chartTop    = valueH + 4 * density;   // room for value labels above bars
        float chartBottom = h - labelH - botPad;    // room for day labels below bars
        float chartH      = chartBottom - chartTop;
        float barAreaW    = (w - leftPad - rightPad) / n;
        float barW        = barAreaW * 0.55f;

        // Horizontal grid lines (3 lines at 25%, 50%, 75% of max)
        int[] gridLevels = {maxValue / 4, maxValue / 2, maxValue * 3 / 4};
        for (int gl : gridLevels) {
            float y = chartBottom - (chartH * gl / maxValue);
            canvas.drawLine(leftPad, y, w - rightPad, y, gridPaint);
        }

        // X-axis line
        canvas.drawLine(leftPad, chartBottom, w - rightPad, chartBottom, axisPaint);

        // Bars + labels
        for (int i = 0; i < n; i++) {
            float cx    = leftPad + barAreaW * i + barAreaW / 2f;
            float barH  = chartH * values[i] / maxValue;
            float left  = cx - barW / 2f;
            float right = cx + barW / 2f;
            float top   = chartBottom - barH;

            boolean isToday = (i == n - 1); // last entry = today
            RectF rect = new RectF(left, top, right, chartBottom);
            canvas.drawRoundRect(rect, barRadius, barRadius, isToday ? todayPaint : barPaint);

            // Value label above bar
            if (values[i] > 0) {
                canvas.drawText(
                        String.valueOf(values[i]),
                        cx,
                        top - 4 * density,
                        valuePaint);
            }

            // Day label below axis (supports two-line with "\n")
            String lbl = labels[i];
            String[] lines = lbl.split("\n");
            float lineH = labelPaint.getTextSize() + 2 * density;
            float labelY = chartBottom + labelPaint.getTextSize() + 4 * density;
            for (String line : lines) {
                canvas.drawText(line, cx, labelY, labelPaint);
                labelY += lineH;
            }
        }
    }
}
