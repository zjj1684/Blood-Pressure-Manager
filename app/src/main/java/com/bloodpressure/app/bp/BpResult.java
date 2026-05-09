package com.bloodpressure.app.bp;

public class BpResult {
    public final int systolic;
    public final int diastolic;
    public final int heartRate;

    public BpResult(int systolic, int diastolic, int heartRate) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.heartRate = heartRate;
    }
}
