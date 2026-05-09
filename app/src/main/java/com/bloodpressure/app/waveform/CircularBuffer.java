package com.bloodpressure.app.waveform;

import com.bloodpressure.app.data.model.SampleData;

import java.util.ArrayList;
import java.util.List;

public class CircularBuffer {

    private final int capacity;
    private final int[] ppgBuffer;
    private final int[] ecgBuffer;
    private int head = 0;
    private int size = 0;
    private final Object lock = new Object();

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        this.ppgBuffer = new int[capacity];
        this.ecgBuffer = new int[capacity];
    }

    public void add(int ppg, int ecg) {
        synchronized (lock) {
            ppgBuffer[head] = ppg;
            ecgBuffer[head] = ecg;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        }
    }

    public void addBatch(List<SampleData> samples) {
        synchronized (lock) {
            for (SampleData sample : samples) {
                ppgBuffer[head] = sample.getPpg();
                ecgBuffer[head] = sample.getEcg();
                head = (head + 1) % capacity;
                if (size < capacity) {
                    size++;
                }
            }
        }
    }

    public int getSize() {
        synchronized (lock) {
            return size;
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int[] getPpgRange(int count) {
        synchronized (lock) {
            int n = Math.min(count, size);
            int[] result = new int[n];
            int start = (head - n + capacity) % capacity;
            for (int i = 0; i < n; i++) {
                result[i] = ppgBuffer[(start + i) % capacity];
            }
            return result;
        }
    }

    public int[] getEcgRange(int count) {
        synchronized (lock) {
            int n = Math.min(count, size);
            int[] result = new int[n];
            int start = (head - n + capacity) % capacity;
            for (int i = 0; i < n; i++) {
                result[i] = ecgBuffer[(start + i) % capacity];
            }
            return result;
        }
    }

    public void clear() {
        synchronized (lock) {
            head = 0;
            size = 0;
        }
    }
}
