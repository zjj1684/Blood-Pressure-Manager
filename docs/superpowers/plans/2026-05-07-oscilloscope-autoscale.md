# Oscilloscope Auto-Scale Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hardcoded 0-4095 display range in `WaveformRenderer` with EMA-smoothed auto-scaling so PPG/ECG signals concentrated in 2000-3000 fill the screen.

**Architecture:** Add a package-private static `computeDisplayRange()` method for testable range computation, then integrate it into the Canvas-based renderer. The public API of `WaveformRenderer.render()` is unchanged — `WaveformView` needs no modifications.

**Tech Stack:** Java 17, Android Canvas, JUnit 4

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `app/src/main/java/com/bloodpressure/app/waveform/WaveformRenderer.java` | Modify | Add auto-scale state fields, `computeDisplayRange()`, update `drawWaveform()` and `render()` |
| `app/src/test/java/com/bloodpressure/app/waveform/WaveformRendererTest.java` | Create | Unit tests for `computeDisplayRange()` logic |

---

### Task 1: Write tests for auto-scale range computation

**Files:**
- Create: `app/src/test/java/com/bloodpressure/app/waveform/WaveformRendererTest.java`

- [ ] **Step 1: Create the test file with tests for `computeDisplayRange()`**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd e:/ProgramAndroid/bloodpressure && ./gradlew test --tests "com.bloodpressure.app.waveform.WaveformRendererTest" 2>&1`
Expected: Compilation failure — `computeDisplayRange` method does not exist yet.

---

### Task 2: Implement auto-scale in WaveformRenderer

**Files:**
- Modify: `app/src/main/java/com/bloodpressure/app/waveform/WaveformRenderer.java`

- [ ] **Step 1: Add state fields and `computeDisplayRange()` method**

Add the following fields after the existing field declarations (after line 22, before the constructor):

```java
private float ppgDisplayMin = 0f;
private float ppgDisplayMax = 4095f;
private float ecgDisplayMin = 0f;
private float ecgDisplayMax = 4095f;
private final float smoothFactor = 0.05f;
private boolean rangeInitialized = false;
```

Add this package-private static method after the `drawGrid()` method (after line 86):

```java
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
    float margin = range * 0.1f;
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
```

- [ ] **Step 2: Modify `drawWaveform()` to accept display range**

Replace the existing `drawWaveform` method (lines 88-106) with:

```java
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
```

- [ ] **Step 3: Update `render()` to compute and pass display ranges**

Replace the existing `render` method (lines 51-76) with:

```java
public void render(Canvas canvas, int[] ppgData, int[] ecgData) {
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Background
    canvas.drawRect(0, 0, width, height, bgPaint);

    // Update display ranges
    float[] ppgRange = computeDisplayRange(ppgData, ppgDisplayMin, ppgDisplayMax,
            smoothFactor, rangeInitialized);
    ppgDisplayMin = ppgRange[0];
    ppgDisplayMax = ppgRange[1];

    float[] ecgRange = computeDisplayRange(ecgData, ecgDisplayMin, ecgDisplayMax,
            smoothFactor, rangeInitialized);
    ecgDisplayMin = ecgRange[0];
    ecgDisplayMax = ecgRange[1];

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
```

- [ ] **Step 4: Run the auto-scale tests**

Run: `cd e:/ProgramAndroid/bloodpressure && ./gradlew test --tests "com.bloodpressure.app.waveform.WaveformRendererTest" 2>&1`
Expected: All 6 tests PASS.

- [ ] **Step 5: Run all existing tests to verify no regressions**

Run: `cd e:/ProgramAndroid/bloodpressure && ./gradlew test 2>&1`
Expected: `BleDataParserTest`, `CircularBufferTest`, and `WaveformRendererTest` all PASS.

---

### Task 3: Verify on device

- [ ] **Step 1: Build the project**

Run: `cd e:/ProgramAndroid/bloodpressure && ./gradlew assembleDebug 2>&1`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Manual verification checklist**

Install on device and connect to BLE device. Verify:
1. PPG waveform (top, red) fills the display vertically instead of appearing flat
2. ECG waveform (bottom, green) fills the display vertically
3. Labels ("PPG", "ECG") and grid lines render correctly
4. Waveform doesn't jitter or jump when signal range changes gradually
5. No crashes or visual artifacts
