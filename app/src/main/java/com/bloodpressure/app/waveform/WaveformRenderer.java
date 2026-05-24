package com.bloodpressure.app.waveform;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class WaveformRenderer {

    private static final int GRID_COLOR = 0x33FFFFFF;
    private static final int PPG_COLOR = 0xFFE8915A;  // 暖橙
    private static final int ECG_COLOR = 0xFF5ABFA0;  // 翠绿
    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int TEXT_COLOR = 0xCCFFFFFF;

    private final Paint gridPaint;
    private final Paint ppgLinePaint;
    private final Paint ecgLinePaint;
    private final Paint bgPaint;
    private final Paint textPaint;
    private final Path ppgPath;
    private final Path ecgPath;

    private static final float DISPLAY_MIN = 1000f;
    private static final float DISPLAY_MAX = 3100f;
    private static final float OUTLIER_THRESHOLD = 300f;
    private static final float ECG_LPF_ALPHA = 0.3f;  // EMA coefficient: lower = more smoothing

    // Pre-allocated buffers to avoid GC pressure
    private int[] filteredBuf = new int[0];
    private float[] ysBuf = new float[0];
    private float ecgEmaState = Float.NaN;  // EMA state for ECG low-pass filter

    // Cached grid bitmap
    private Bitmap gridBitmap;
    private int cachedWidth;
    private int cachedHeight;

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

    public void render(Canvas canvas, int[] ppgData, int ppgLen, int[] ecgData, int ecgLen) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Background
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Draw cached grid (only redrawn when size changes)
        if (gridBitmap == null || cachedWidth != width || cachedHeight != height) {
            buildGridBitmap(width, height);
        }
        canvas.drawBitmap(gridBitmap, 0, 0, null);

        int halfHeight = height / 2;

        // Draw labels
        canvas.drawText("PPG", 10, 30, textPaint);
        canvas.drawText("ECG", 10, halfHeight + 30, textPaint);

        // Draw waveforms
        if (ppgData != null && ppgLen > 1) {
            drawWaveform(canvas, ppgData, ppgLen, ppgPath, ppgLinePaint, width, halfHeight, 0,
                    DISPLAY_MIN, DISPLAY_MAX, false);
        }
        if (ecgData != null && ecgLen > 1) {
            drawWaveform(canvas, ecgData, ecgLen, ecgPath, ecgLinePaint, width, halfHeight, halfHeight,
                    DISPLAY_MIN, DISPLAY_MAX, true);
        }
    }

    private void buildGridBitmap(int width, int height) {
        gridBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(gridBitmap);
        int gridSpacing = 50;
        for (int x = 0; x < width; x += gridSpacing) {
            c.drawLine(x, 0, x, height, gridPaint);
        }
        for (int y = 0; y <= height; y += gridSpacing) {
            c.drawLine(0, y, width, y, gridPaint);
        }
        cachedWidth = width;
        cachedHeight = height;
    }

    private void drawWaveform(Canvas canvas, int[] data, int n, Path path, Paint paint,
                               int width, int height, int offsetY,
                               float displayMin, float displayMax, boolean applyEma) {
        path.reset();

        float range = displayMax - displayMin;
        if (range < 1f) range = 1f;
        float xStep = (float) width / n;

        // Ensure pre-allocated buffers are large enough
        if (filteredBuf.length < n) {
            filteredBuf = new int[n];
            ysBuf = new float[n];
        }

        // Filter outliers in-place
        filteredBuf[0] = data[0];
        for (int i = 1; i < n - 1; i++) {
            int prev = filteredBuf[i - 1];
            int cur = data[i];
            int next = data[i + 1];
            int median = Math.max(Math.min(prev, cur), Math.min(Math.max(prev, cur), next));
            if (Math.abs(cur - median) > OUTLIER_THRESHOLD) {
                filteredBuf[i] = (prev + next) / 2;
            } else {
                filteredBuf[i] = cur;
            }
        }
        filteredBuf[n - 1] = data[n - 1];

        // EMA low-pass filter for ECG to reduce high-frequency noise
        if (applyEma) {
            if (Float.isNaN(ecgEmaState)) {
                ecgEmaState = filteredBuf[0];
            }
            for (int i = 0; i < n; i++) {
                ecgEmaState = ECG_LPF_ALPHA * filteredBuf[i] + (1f - ECG_LPF_ALPHA) * ecgEmaState;
                filteredBuf[i] = Math.round(ecgEmaState);
            }
        }

        for (int i = 0; i < n; i++) {
            ysBuf[i] = offsetY + height * (1f - clamp((filteredBuf[i] - displayMin) / range));
        }

        path.moveTo(0, ysBuf[0]);
        for (int i = 1; i < n; i++) {
            path.lineTo(i * xStep, ysBuf[i]);
        }

        canvas.drawPath(path, paint);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public void resetFilterState() {
        ecgEmaState = Float.NaN;
    }

    public void release() {
        if (gridBitmap != null) {
            gridBitmap.recycle();
            gridBitmap = null;
        }
    }
}
