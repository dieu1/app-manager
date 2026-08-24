package com.vandieu_manhdung.taskmanager.ui.team.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.util.GanttLayoutRules;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeamGanttChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final List<TeamTaskItem> items = new ArrayList<>();
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
    private final float density;
    private final int labelWidth;
    private final int dayWidth;
    private final int headerHeight;
    private final int rowHeight;
    private long rangeStart;
    private int dayCount = 7;

    public TeamGanttChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        labelWidth = dp(156);
        dayWidth = dp(46);
        headerHeight = dp(46);
        rowHeight = dp(58);
        setBackgroundColor(ContextCompat.getColor(context, R.color.team_surface));
    }

    public void setItems(List<TeamTaskItem> values) {
        items.clear();
        if (values != null) items.addAll(values);
        List<Task> tasks = new ArrayList<>();
        for (TeamTaskItem item : items) tasks.add(item.getTask());
        long[] range = GanttLayoutRules.range(tasks, System.currentTimeMillis());
        rangeStart = range[0];
        dayCount = Math.min(366, GanttLayoutRules.inclusiveDayCount(range[0], range[1]));
        setContentDescription(getResources().getQuantityString(
                R.plurals.gantt_task_count, items.size(), items.size()));
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = labelWidth + Math.max(7, dayCount) * dayWidth;
        int desiredHeight = headerHeight + Math.max(1, items.size()) * rowHeight;
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
        if (items.isEmpty()) {
            drawText(canvas, getContext().getString(R.string.no_gantt_tasks),
                    dp(16), headerHeight + dp(34), R.color.team_text_secondary, 14, false);
            return;
        }
        long now = System.currentTimeMillis();
        for (int index = 0; index < items.size(); index++) {
            drawTask(canvas, items.get(index), index, now);
        }
    }

    private void drawGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.team_surface_alt));
        canvas.drawRect(0, 0, getWidth(), headerHeight, paint);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.team_border));
        paint.setStrokeWidth(dp(1));
        for (int day = 0; day <= Math.max(7, dayCount); day++) {
            float x = labelWidth + day * dayWidth;
            canvas.drawLine(x, 0, x, getHeight(), paint);
            if (day < dayCount) {
                drawText(canvas,
                        dayFormat.format(new Date(rangeStart + day * GanttLayoutRules.DAY_MILLIS)),
                        x + dp(6), dp(28), R.color.team_text_secondary, 11, true);
            }
        }
        for (int row = 0; row <= Math.max(1, items.size()); row++) {
            float y = headerHeight + row * rowHeight;
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
        canvas.drawLine(labelWidth, 0, labelWidth, getHeight(), paint);
    }

    private void drawTask(Canvas canvas, TeamTaskItem item, int row, long now) {
        Task task = item.getTask();
        float top = headerHeight + row * rowHeight;
        drawEllipsized(canvas, task.getTitle(), dp(12), top + dp(23), labelWidth - dp(20),
                R.color.team_text_primary, 13, true);
        String names = item.getAssigneeName();
        drawEllipsized(canvas, names == null ? "" : names, dp(12), top + dp(43),
                labelWidth - dp(20), R.color.team_text_secondary, 10, false);

        long start = GanttLayoutRules.taskStart(task, now);
        long end = GanttLayoutRules.taskEnd(task, start);
        int startDay = Math.min(dayCount - 1, GanttLayoutRules.dayIndex(rangeStart, start));
        int endDay = Math.min(dayCount - 1, GanttLayoutRules.dayIndex(rangeStart, end));
        float left = labelWidth + startDay * dayWidth + dp(5);
        float right = labelWidth + (endDay + 1) * dayWidth - dp(5);
        float barTop = top + dp(17);
        float barBottom = top + dp(41);
        RectF full = new RectF(left, barTop, Math.max(left + dp(14), right), barBottom);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.team_primary_soft));
        canvas.drawRoundRect(full, dp(8), dp(8), paint);
        RectF completed = new RectF(full.left, full.top,
                full.left + full.width() * GanttLayoutRules.progressFraction(task.getProgress()),
                full.bottom);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.team_primary));
        canvas.drawRoundRect(completed, dp(8), dp(8), paint);
        drawText(canvas, task.getProgress() + "%", full.left + dp(7), top + dp(34),
                R.color.team_text_primary, 10, true);
    }

    private void drawEllipsized(Canvas canvas, String value, float x, float y, float maxWidth,
                                int color, float sizeSp, boolean bold) {
        configureText(color, sizeSp, bold);
        CharSequence text = TextUtils.ellipsize(value, textPaint, maxWidth,
                TextUtils.TruncateAt.END);
        canvas.drawText(text.toString(), x, y, textPaint);
    }

    private void drawText(Canvas canvas, String value, float x, float y,
                          int color, float sizeSp, boolean bold) {
        configureText(color, sizeSp, bold);
        canvas.drawText(value, x, y, textPaint);
    }

    private void configureText(int color, float sizeSp, boolean bold) {
        textPaint.setColor(ContextCompat.getColor(getContext(), color));
        textPaint.setTextSize(sizeSp * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setFakeBoldText(bold);
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
