package com.bloodpressure.app.waveform;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {

    private static final int BUFFER_CAPACITY = 5000;
    private static final int VISIBLE_SAMPLES = 1000;
    private static final long REFRESH_INTERVAL_MS = 16;

    private final CircularBuffer buffer;
    private final WaveformRenderer renderer;
    private boolean isRunning = false;

    public WaveformView(Context context) {
        this(context, null);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        buffer = new CircularBuffer(BUFFER_CAPACITY);
        renderer = new WaveformRenderer();
    }

    public void addSamples(int ppg, int ecg) {
        buffer.add(ppg, ecg);
        if (!isRunning) {
            isRunning = true;
            postInvalidateDelayed(REFRESH_INTERVAL_MS);
        }
    }

    public void addBatch(java.util.List<com.bloodpressure.app.data.model.SampleData> samples) {
        buffer.addBatch(samples);
        if (!isRunning) {
            isRunning = true;
            postInvalidateDelayed(REFRESH_INTERVAL_MS);
        }
    }

    public void clear() {
        buffer.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int[] ppgData = buffer.getPpgRange(VISIBLE_SAMPLES);
        int[] ecgData = buffer.getEcgRange(VISIBLE_SAMPLES);

        renderer.render(canvas, ppgData, ecgData);

        if (buffer.getSize() > 0) {
            postInvalidateDelayed(REFRESH_INTERVAL_MS);
        } else {
            isRunning = false;
        }
    }
}
