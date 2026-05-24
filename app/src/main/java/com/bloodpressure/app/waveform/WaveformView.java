package com.bloodpressure.app.waveform;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

public class WaveformView extends View {

    private static final int BUFFER_CAPACITY = 5000;
    private static final int VISIBLE_SAMPLES = 1500;

    private final CircularBuffer buffer;
    private final WaveformRenderer renderer;

    // Pre-allocated display buffers — reused every frame
    private final int[] ppgDisplay = new int[VISIBLE_SAMPLES];
    private final int[] ecgDisplay = new int[VISIBLE_SAMPLES];

    private boolean isRunning = false;

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isRunning) return;
            invalidate();
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

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
        startIfNeeded();
    }

    public void addBatch(java.util.List<com.bloodpressure.app.data.model.SampleData> samples) {
        buffer.addBatch(samples);
        startIfNeeded();
    }

    private void startIfNeeded() {
        if (!isRunning && isAttachedToWindow()) {
            isRunning = true;
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
    }

    public void clear() {
        isRunning = false;
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        buffer.clear();
        renderer.resetFilterState();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int ppgLen = buffer.copyPpgRange(ppgDisplay, VISIBLE_SAMPLES);
        int ecgLen = buffer.copyEcgRange(ecgDisplay, VISIBLE_SAMPLES);

        renderer.render(canvas, ppgDisplay, ppgLen, ecgDisplay, ecgLen);

        if (buffer.getSize() == 0) {
            isRunning = false;
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isRunning = false;
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        renderer.release();
    }
}
