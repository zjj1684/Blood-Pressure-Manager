package com.bloodpressure.app.waveform;

import org.junit.Test;

import static org.junit.Assert.*;

public class WaveformRendererTest {

    private static final float SMOOTH_FACTOR = 0.05f;

    @Test
    public void computeDisplayRange_firstCall_seedsFromData() {
        int[] data = {2000, 2500, 3000};
        float[] result = WaveformRenderer.computeDisplayRange(
                data, 0f, 4095f, SMOOTH_FACTOR, false);

        // First call should seed directly: range = 2000-3000, margin = 100
        // min = 2000 - 100 = 1900, max = 3000 + 100 = 3100
        assertEquals(1900f, result[0], 1f);
        assertEquals(3100f, result[1], 1f);
    }

    @Test
    public void computeDisplayRange_subsequentCall_appliesSmoothing() {
        int[] data = {2000, 2500, 3000};

        // First call: seeds to 1900-3100
        float[] result = WaveformRenderer.computeDisplayRange(
                data, 0f, 4095f, SMOOTH_FACTOR, false);
        float firstMin = result[0];
        float firstMax = result[1];

        // Second call with same data: should stay near 1900-3100 (no change)
        result = WaveformRenderer.computeDisplayRange(
                data, firstMin, firstMax, SMOOTH_FACTOR, true);
        assertEquals(firstMin, result[0], 1f);
        assertEquals(firstMax, result[1], 1f);
    }

    @Test
    public void computeDisplayRange_shiftedData_adaptsSmoothly() {
        int[] data = {2000, 2500, 3000};

        // Seed
        float[] result = WaveformRenderer.computeDisplayRange(
                data, 0f, 4095f, SMOOTH_FACTOR, false);

        // Shift data to 1000-1500
        int[] shifted = {1000, 1250, 1500};
        result = WaveformRenderer.computeDisplayRange(
                shifted, result[0], result[1], SMOOTH_FACTOR, true);

        // Should move toward 950-1650 but not jump there (EMA smoothing)
        assertTrue("min should decrease", result[0] < 1900f);
        assertTrue("min should not jump fully", result[0] > 950f);
        assertTrue("max should decrease", result[1] < 3100f);
    }

    @Test
    public void computeDisplayRange_flatSignal_returnsSafeRange() {
        int[] data = {2500, 2500, 2500};
        float[] result = WaveformRenderer.computeDisplayRange(
                data, 0f, 4095f, SMOOTH_FACTOR, false);

        // range = 0, margin = 0; should still return valid min/max
        assertTrue("max > min", result[1] > result[0]);
        assertTrue("range at least 1", result[1] - result[0] >= 1f);
    }

    @Test
    public void computeDisplayRange_emptyData_unchanged() {
        int[] data = {};
        float[] result = WaveformRenderer.computeDisplayRange(
                data, 1000f, 3000f, SMOOTH_FACTOR, true);

        // Empty data should not change the range
        assertEquals(1000f, result[0], 0.01f);
        assertEquals(3000f, result[1], 0.01f);
    }

    @Test
    public void computeDisplayRange_singleValue_returnsSafeRange() {
        int[] data = {2500};
        float[] result = WaveformRenderer.computeDisplayRange(
                data, 0f, 4095f, SMOOTH_FACTOR, false);

        // Single value: min == max == 2500, margin = 0
        // Should add guard range
        assertTrue("max > min", result[1] > result[0]);
    }
}
