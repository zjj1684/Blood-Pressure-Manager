package com.bloodpressure.app.waveform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

public class WaveformRenderer {

    private static final int GRID_COLOR = 0x33FFFFFF;
    private static final int PPG_COLOR = Color.RED;
    private static final int ECG_COLOR = Color.GREEN;
    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int TEXT_COLOR = Color.WHITE;

    private final Paint gridPaint;
    private final Paint ppgLinePaint;
    private final Paint ecgLinePaint;
    private final Paint bgPaint;
    private final Paint textPaint;
    private final Path ppgPath;
    private final Path ecgPath;

    private float ppgDisplayMin = 0f;
    private float ppgDisplayMax = 4095f;
    private float ecgDisplayMin = 0f;
    private float ecgDisplayMax = 4095f;
    private static final float SMOOTH_FACTOR = 0.05f;
    private static final float RANGE_MARGIN_RATIO = 0.1f;
    private boolean rangeInitialized = false;

    public WaveformRenderer() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(GRID_COLOR);
        gridPaint.setStrokeWidth(1f);

        ppgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ppgLinePaint.setColor(PPG_COLOR);
        ppgLinePaint.setStrokeWidth(2f);
        ppgLinePaint.setStyle(Paint.Style.STROKE);

        ecgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ecgLinePaint.setColor(ECG_COLOR);
        ecgLinePaint.setStrokeWidth(2f);
        ecgLinePaint.setStyle(Paint.Style.STROKE);

        bgPaint = new Paint();
        bgPaint.setColor(BG_COLOR);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(28f);

        ppgPath = new Path();
        ecgPath = new Path();
    }

    public void render(Canvas canvas, int[] ppgData, int[] ecgData) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Background
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Update display ranges
        if (ppgData != null) {
            float[] ppgRange = computeDisplayRange(ppgData, ppgDisplayMin, ppgDisplayMax,
                    SMOOTH_FACTOR, rangeInitialized);
            ppgDisplayMin = ppgRange[0];
            ppgDisplayMax = ppgRange[1];
        }

        if (ecgData != null) {
            float[] ecgRange = computeDisplayRange(ecgData, ecgDisplayMin, ecgDisplayMax,
                    SMOOTH_FACTOR, rangeInitialized);
            ecgDisplayMin = ecgRange[0];
            ecgDisplayMax = ecgRange[1];
        }

        rangeInitialized = true;

        // Split into two halves
        int halfHeight = height / 2;

        // Draw grid
        drawGrid(canvas, width, halfHeight, 0);
        drawGrid(canvas, width, halfHeight, halfHeight);

        // Draw labels
        canvas.drawText("PPG", 10, 30, textPaint);
        canvas.drawText("ECG", 10, halfHeight + 30, textPaint);

        // Draw waveforms
        if (ppgData != null && ppgData.length > 1) {
            drawWaveform(canvas, ppgData, ppgPath, ppgLinePaint, width, halfHeight, 0,
                    ppgDisplayMin, ppgDisplayMax);
        }
        if (ecgData != null && ecgData.length > 1) {
            drawWaveform(canvas, ecgData, ecgPath, ecgLinePaint, width, halfHeight, halfHeight,
                    ecgDisplayMin, ecgDisplayMax);
        }
    }

    private void drawGrid(Canvas canvas, int width, int height, int offsetY) {
        int gridSpacing = 50;
        for (int x = 0; x < width; x += gridSpacing) {
            canvas.drawLine(x, offsetY, x, offsetY + height, gridPaint);
        }
        for (int y = offsetY; y <= offsetY + height; y += gridSpacing) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }
    }

    private void drawWaveform(Canvas canvas, int[] data, Path path, Paint paint,
                               int width, int height, int offsetY,
                               float displayMin, float displayMax) {
        path.reset();

        float range = displayMax - displayMin;
        if (range < 1f) range = 1f;
        float xStep = (float) width / data.length;

        float normalized = (data[0] - displayMin) / range;
        path.moveTo(0, offsetY + height * (1f - normalized));

        for (int i = 1; i < data.length; i++) {
            float x = i * xStep;
            normalized = (data[i] - displayMin) / range;
            float y = offsetY + height * (1f - normalized);
            path.lineTo(x, y);
        }

        canvas.drawPath(path, paint);
    }

    static float[] computeDisplayRange(int[] data, float currentMin, float currentMax,
                                        float smoothFactor, boolean initialized) {
        if (data.length == 0) {
            return new float[]{currentMin, currentMax};
        }

        int actualMin = Integer.MAX_VALUE;
        int actualMax = Integer.MIN_VALUE;
        for (int v : data) {
            if (v < actualMin) actualMin = v;
            if (v > actualMax) actualMax = v;
        }

        float range = actualMax - actualMin;
        float margin = range * RANGE_MARGIN_RATIO;
        float targetMin = actualMin - margin;
        float targetMax = actualMax + margin;

        // Ensure minimum range of 1 to prevent division by zero
        if (targetMax - targetMin < 1f) {
            float center = (targetMax + targetMin) / 2f;
            targetMin = center - 0.5f;
            targetMax = center + 0.5f;
        }

        if (!initialized) {
            return new float[]{targetMin, targetMax};
        }

        float newMin = currentMin + smoothFactor * (targetMin - currentMin);
        float newMax = currentMax + smoothFactor * (targetMax - currentMax);
        return new float[]{newMin, newMax};
    }
}
