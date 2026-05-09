package com.bloodpressure.app.bp;

public interface BpCalculator {
    BpResult calculate(int[] ppgBuffer, int[] ecgBuffer, int sampleRate);
}
