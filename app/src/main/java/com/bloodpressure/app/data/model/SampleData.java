package com.bloodpressure.app.data.model;

public class SampleData {
    private final int ppg;
    private final int ecg;

    public SampleData(int ppg, int ecg) {
        this.ppg = ppg;
        this.ecg = ecg;
    }

    public int getPpg() { return ppg; }
    public int getEcg() { return ecg; }
}
