# Oscilloscope Auto-Scale Optimization

## Context

The oscilloscope waveform display (`WaveformRenderer`) maps raw 12-bit ADC values (0-4095) directly to screen Y-coordinates. However, PPG and ECG signals from the CBN:BP01 device are concentrated in the 2000-3000 range. This means the waveform only occupies ~24% of the display height, appearing nearly flat and unreadable.

The goal is to make the waveform fill the available display area by dynamically adapting to the actual signal range.

## Approach: EMA-Smoothed Auto-Scale

Use Exponential Moving Average (EMA) to track the actual signal range and smoothly adapt the display mapping. Only the rendering layer changes — no modifications to BLE, data storage, or the circular buffer.

## Changes

### File: `app/src/main/java/com/bloodpressure/app/waveform/WaveformRenderer.java`

**1. Add state fields:**

```java
private float ppgDisplayMin = 0f;
private float ppgDisplayMax = 4095f;
private float ecgDisplayMin = 0f;
private float ecgDisplayMax = 4095f;
private final float smoothFactor = 0.05f;
private boolean initialized = false;
```

**2. Add `updateDisplayRange()` method:**

- Scans the visible data array for actual min/max values
- Adds 10% margin around the range to prevent edge clipping
- On first call, seeds the display range directly from actual data
- On subsequent calls, applies EMA smoothing: `displayMin += α * (targetMin - displayMin)`

**3. Modify `drawWaveform()` signature:**

- Add `float displayMin, float displayMax` parameters
- Replace the hardcoded `float maxVal = 4095f` normalization
- New mapping: `normalized = (value - displayMin) / (displayMax - displayMin)` → 0.0 to 1.0
- Y-coordinate: `y = offsetY + height * (1f - normalized)`
- Guard against division by zero when `displayMax - displayMin < 1f`

**4. Update `render()` to:**

- Call `updateDisplayRange()` for PPG and ECG data separately
- Pass the smoothed `displayMin`/`displayMax` to each `drawWaveform()` call

### No changes to:

- `WaveformView.java` — still calls `renderer.render(canvas, ppgData, ecgData)`
- `CircularBuffer.java` — still stores raw int values
- `BleDataParser.java` — still parses 12-bit ADC values
- `BleService.java`, `MonitorFragment.java` — data flow unchanged
- Database layer — raw values still persisted

## Key Parameters

| Parameter | Value | Purpose |
|-----------|-------|---------|
| EMA α (smoothFactor) | 0.05 | Range adapts over ~20 frames (~330ms). Fast enough for breathing/movement, slow enough to avoid jitter. |
| Margin | 10% of (max-min) | Prevents waveform from touching exact top/bottom edges. |
| Range guard | < 1f | Prevents division by zero when signal is perfectly flat. |

## Behavior

- **Signal at 2000-3000**: Display auto-zooms, waveform fills the screen
- **Signal shifts to 1800-2800**: Display smoothly follows over ~300ms
- **Single outlier spike at 4000**: EMA smooths it out, display doesn't jump
- **No data / flat signal**: Guard prevents division by zero, waveform stays at center

## Verification

1. Connect to BLE device, start monitoring
2. Verify PPG waveform (top, red) fills the display vertically instead of appearing flat
3. Verify ECG waveform (bottom, green) fills the display vertically
4. Verify labels ("PPG", "ECG") and grid lines still render correctly
5. Verify the waveform doesn't jitter or jump when signal range changes gradually
6. Run existing unit tests: `BleDataParserTest`, `CircularBufferTest` (should pass unchanged since only rendering changed)
