package com.bloodpressure.app.waveform;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

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

    private static final float DISPLAY_MIN = 1100f;
    private static final float DISPLAY_MAX = 3000f;

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
                    DISPLAY_MIN, DISPLAY_MAX);
        }
        if (ecgData != null && ecgData.length > 1) {
            drawWaveform(canvas, ecgData, ecgPath, ecgLinePaint, width, halfHeight, halfHeight,
                    DISPLAY_MIN, DISPLAY_MAX);
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

    private static final float OUTLIER_THRESHOLD = 300f;

    private void drawWaveform(Canvas canvas, int[] data, Path path, Paint paint,
                               int width, int height, int offsetY,
                               float displayMin, float displayMax) {
        path.reset();

        float range = displayMax - displayMin;
        if (range < 1f) range = 1f;
        float xStep = (float) width / data.length;
        int n = data.length;
        if (n < 2) return;

        // Copy and filter outliers
        int[] filtered = new int[n];
        filtered[0] = data[0];
        for (int i = 1; i < n - 1; i++) {
            int prev = filtered[i - 1];
            int cur = data[i];
            int next = data[i + 1];
            int median = Math.max(Math.min(prev, cur), Math.min(Math.max(prev, cur), next));
            if (Math.abs(cur - median) > OUTLIER_THRESHOLD) {
                filtered[i] = (prev + next) / 2;
            } else {
                filtered[i] = cur;
            }
        }
        filtered[n - 1] = data[n - 1];

        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            ys[i] = offsetY + height * (1f - clamp((filtered[i] - displayMin) / range));
        }

        path.moveTo(0, ys[0]);

        for (int i = 1; i < n; i++) {
            path.lineTo(i * xStep, ys[i]);
        }

        canvas.drawPath(path, paint);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

}
