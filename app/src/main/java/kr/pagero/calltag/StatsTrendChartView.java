package kr.pagero.calltag;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** 7~30일 통화·페이지로 유입 추이를 보여주며 터치한 날짜의 정확한 수치를 표시한다. */
public final class StatsTrendChartView extends View {
    private String[] labels = new String[0];
    private int[] calls = new int[0];
    private int[] leads = new int[0];
    private int selectedIndex = -1;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint callsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leadsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipDatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StatsTrendChartView(Context context) {
        super(context);
        init();
    }

    public StatsTrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatsTrendChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        gridPaint.setColor(getContext().getColor(R.color.border));
        gridPaint.setStrokeWidth(dp(1));

        axisTextPaint.setColor(getContext().getColor(R.color.text_muted));
        axisTextPaint.setTextSize(sp(10));

        callsPaint.setColor(getContext().getColor(R.color.primary));
        callsPaint.setStyle(Paint.Style.STROKE);
        callsPaint.setStrokeWidth(dp(2.5f));
        callsPaint.setStrokeCap(Paint.Cap.ROUND);
        callsPaint.setStrokeJoin(Paint.Join.ROUND);

        leadsPaint.setColor(getContext().getColor(R.color.success));
        leadsPaint.setStyle(Paint.Style.STROKE);
        leadsPaint.setStrokeWidth(dp(2.5f));
        leadsPaint.setStrokeCap(Paint.Cap.ROUND);
        leadsPaint.setStrokeJoin(Paint.Join.ROUND);

        pointPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(getContext().getColor(R.color.surface));

        selectionPaint.setColor(getContext().getColor(R.color.text_secondary));
        selectionPaint.setStrokeWidth(dp(1));
        selectionPaint.setStyle(Paint.Style.STROKE);

        // 툴팁은 차트 배경과 확실히 분리되는 어두운 패널 + 밝은 글자로 고정한다.
        tooltipPaint.setColor(getContext().getColor(R.color.surface_soft));
        tooltipPaint.setStyle(Paint.Style.FILL);
        tooltipStrokePaint.setColor(getContext().getColor(R.color.primary));
        tooltipStrokePaint.setStyle(Paint.Style.STROKE);
        tooltipStrokePaint.setStrokeWidth(dp(1));

        tooltipTextPaint.setColor(getContext().getColor(R.color.text_primary));
        tooltipTextPaint.setTextSize(sp(12.5f));
        tooltipTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        tooltipDatePaint.setColor(getContext().getColor(R.color.text_secondary));
        tooltipDatePaint.setTextSize(sp(10.5f));
        tooltipDatePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(String[] labels, int[] calls, int[] leads) {
        this.labels = labels == null ? new String[0] : labels;
        this.calls = calls == null ? new int[0] : calls;
        this.leads = leads == null ? new int[0] : leads;
        selectedIndex = -1;
        int callTotal = sum(this.calls);
        int leadTotal = sum(this.leads);
        setContentDescription("일별 통화 추이 " + callTotal + "건, 페이지로 유입 " + leadTotal + "명. 차트를 누르면 날짜별 수치를 확인할 수 있습니다.");
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int count = Math.min(labels.length, Math.min(calls.length, leads.length));
        if (count == 0) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_MOVE
                || event.getActionMasked() == MotionEvent.ACTION_UP) {
            float left = dp(34);
            float right = getWidth() - dp(12);
            float width = Math.max(1f, right - left);
            float step = count <= 1 ? width : width / (count - 1f);
            int index = count <= 1 ? 0 : Math.round((event.getX() - left) / step);
            selectedIndex = Math.max(0, Math.min(count - 1, index));
            setContentDescription(detailText(selectedIndex));
            invalidate();
            if (event.getActionMasked() == MotionEvent.ACTION_UP) performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        RectF surface = new RectF(0, 0, width, height);
        canvas.drawRoundRect(surface, dp(15), dp(15), backgroundPaint);

        float left = dp(34);
        float right = width - dp(12);
        float top = dp(48);
        float bottom = height - dp(28);
        float chartWidth = Math.max(1f, right - left);
        float chartHeight = Math.max(1f, bottom - top);

        drawLegend(canvas);

        int max = 1;
        for (int value : calls) max = Math.max(max, value);
        for (int value : leads) max = Math.max(max, value);
        int roundedMax = roundedMax(max);

        for (int i = 0; i <= 3; i++) {
            float ratio = i / 3f;
            float y = bottom - chartHeight * ratio;
            canvas.drawLine(left, y, right, y, gridPaint);
            int label = Math.round(roundedMax * ratio);
            String text = String.valueOf(label);
            axisTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(text, left - dp(7), y + dp(3), axisTextPaint);
        }

        int count = Math.min(labels.length, Math.min(calls.length, leads.length));
        if (count == 0) return;
        float step = count <= 1 ? 0f : chartWidth / (count - 1f);

        drawSeries(canvas, calls, count, left, bottom, step, chartHeight, roundedMax,
                callsPaint, getContext().getColor(R.color.primary));
        drawSeries(canvas, leads, count, left, bottom, step, chartHeight, roundedMax,
                leadsPaint, getContext().getColor(R.color.success));

        int labelStep = count <= 7 ? 1 : 5;
        axisTextPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < count; i++) {
            if (i % labelStep != 0 && i != count - 1) continue;
            float x = left + step * i;
            canvas.drawText(labels[i], x, height - dp(10), axisTextPaint);
        }

        if (selectedIndex >= 0 && selectedIndex < count) {
            drawSelection(canvas, selectedIndex, left, top, bottom, step, chartWidth);
        }
    }

    private void drawLegend(Canvas canvas) {
        float y = dp(22);
        pointPaint.setColor(getContext().getColor(R.color.primary));
        canvas.drawCircle(dp(17), y - dp(3), dp(4), pointPaint);
        axisTextPaint.setTextAlign(Paint.Align.LEFT);
        axisTextPaint.setTextSize(sp(11));
        axisTextPaint.setColor(getContext().getColor(R.color.text_secondary));
        canvas.drawText("통화", dp(27), y, axisTextPaint);

        pointPaint.setColor(getContext().getColor(R.color.success));
        canvas.drawCircle(dp(82), y - dp(3), dp(4), pointPaint);
        canvas.drawText("페이지로 유입", dp(92), y, axisTextPaint);
        axisTextPaint.setTextSize(sp(10));
        axisTextPaint.setColor(getContext().getColor(R.color.text_muted));
    }

    private void drawSelection(Canvas canvas, int index, float left, float top,
                               float bottom, float step, float chartWidth) {
        float x = labels.length <= 1 ? left + chartWidth / 2f : left + step * index;
        canvas.drawLine(x, top, x, bottom, selectionPaint);

        String valueLine = "통화 " + calls[index] + "건  ·  페이지로 " + leads[index] + "명";
        float widest = Math.max(tooltipTextPaint.measureText(valueLine),
                tooltipDatePaint.measureText(labels[index]));
        float tooltipWidth = Math.min(getWidth() - dp(16), Math.max(dp(172), widest + dp(28)));
        float boxLeft = Math.max(dp(8),
                Math.min(x - tooltipWidth / 2f, getWidth() - dp(8) - tooltipWidth));
        RectF box = new RectF(boxLeft, dp(30), boxLeft + tooltipWidth, dp(78));
        canvas.drawRoundRect(box, dp(10), dp(10), tooltipPaint);
        canvas.drawRoundRect(box, dp(10), dp(10), tooltipStrokePaint);
        canvas.drawText(labels[index], box.centerX(), dp(48), tooltipDatePaint);
        canvas.drawText(valueLine, box.centerX(), dp(68), tooltipTextPaint);
    }

    private String detailText(int index) {
        if (index < 0 || index >= labels.length || index >= calls.length || index >= leads.length) {
            return "";
        }
        return labels[index] + " · 통화 " + calls[index] + "건 · 페이지로 " + leads[index] + "명";
    }

    private void drawSeries(Canvas canvas, int[] values, int count,
                            float left, float bottom, float step, float chartHeight,
                            int max, Paint linePaint, int pointColor) {
        Path path = new Path();
        for (int i = 0; i < count; i++) {
            float x = left + step * i;
            float y = bottom - chartHeight * values[i] / Math.max(1f, max);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);

        pointPaint.setColor(pointColor);
        int pointStep = count <= 7 ? 1 : 5;
        for (int i = 0; i < count; i++) {
            if (i % pointStep != 0 && i != count - 1) continue;
            float x = left + step * i;
            float y = bottom - chartHeight * values[i] / Math.max(1f, max);
            canvas.drawCircle(x, y, dp(3.2f), pointPaint);
        }
    }

    private int roundedMax(int value) {
        if (value <= 3) return 3;
        if (value <= 5) return 5;
        if (value <= 10) return 10;
        return ((value + 4) / 5) * 5;
    }

    private int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
