package com.bloodpressure.app.ble;

import com.bloodpressure.app.data.model.SampleData;

import java.util.ArrayList;
import java.util.List;

public class BleDataParser {

    private static final int PACKET_TYPE_DATA = 0x02;
    private static final int PACKET_SIZE = 17;
    private static final int SAMPLES_PER_PACKET = 5;
    private static final int DATA_OFFSET = 2;

    private int lastSeq = -1;
    private int droppedPackets = 0;

    public ParseResult parse(byte[] data) {
        if (data == null || data.length < PACKET_SIZE) {
            return null;
        }

        int type = data[0] & 0xFF;
        if (type != PACKET_TYPE_DATA) {
            return null;
        }

        int seq = data[1] & 0xFF;
        if (lastSeq >= 0) {
            int expectedSeq = (lastSeq + 1) & 0xFF;
            if (seq != expectedSeq) {
                droppedPackets++;
            }
        }
        lastSeq = seq;

        List<SampleData> samples = new ArrayList<>(SAMPLES_PER_PACKET);
        for (int i = 0; i < SAMPLES_PER_PACKET; i++) {
            int offset = DATA_OFFSET + i * 3;
            if (offset + 2 >= data.length) break;

            int ppgHigh = data[offset] & 0xFF;
            int mid = data[offset + 1] & 0xFF;
            int ecgLow = data[offset + 2] & 0xFF;

            int ppg = (ppgHigh << 4) | (mid >> 4);
            int ecg = ((mid & 0x0F) << 8) | ecgLow;

            samples.add(new SampleData(ppg, ecg));
        }

        return new ParseResult(samples, seq, droppedPackets);
    }

    public void reset() {
        lastSeq = -1;
        droppedPackets = 0;
    }

    public int getDroppedPackets() {
        return droppedPackets;
    }

    public static class ParseResult {
        public final List<SampleData> samples;
        public final int seq;
        public final int totalDropped;

        public ParseResult(List<SampleData> samples, int seq, int totalDropped) {
            this.samples = samples;
            this.seq = seq;
            this.totalDropped = totalDropped;
        }
    }
}
