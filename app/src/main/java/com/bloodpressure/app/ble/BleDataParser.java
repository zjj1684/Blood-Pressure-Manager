package com.bloodpressure.app.ble;

import com.bloodpressure.app.data.model.SampleData;

import java.util.ArrayList;
import java.util.List;

public class BleDataParser {

    private static final int PACKET_TYPE_DATA = 0x02;
    private static final int PACKET_SIZE = 5;
    private static final int SAMPLES_PER_PACKET = 1;
    private static final int DATA_OFFSET = 2;

    private int lastSeq = -1;
    private int droppedPackets = 0;

    public ParseResult parse(byte[] data) {
        if (data == null || data.length < PACKET_SIZE) {
            return null;
        }

        // Scan for all candidate boundaries (0x02 type with 0x02 at +5),
        // then prefer the one whose sequence follows lastSeq.
        int firstCandidate = -1;
        int bestCandidate = -1;
        int scanExpectedSeq = (lastSeq + 1) & 0xFF;

        for (int i = 0; i + PACKET_SIZE < data.length; i++) {
            if ((data[i] & 0xFF) == PACKET_TYPE_DATA
                    && (data[i + PACKET_SIZE] & 0xFF) == PACKET_TYPE_DATA) {
                if (firstCandidate < 0) {
                    firstCandidate = i;
                }
                int candidateSeq = data[i + 1] & 0xFF;
                if (lastSeq < 0 || candidateSeq == scanExpectedSeq) {
                    bestCandidate = i;
                    break;
                }
            }
        }

        int start;
        if (bestCandidate >= 0) {
            start = bestCandidate;
        } else if (firstCandidate >= 0) {
            start = firstCandidate;
        } else {
            return null;
        }

        // Parse all complete 5-byte packets from start
        List<SampleData> samples = new ArrayList<>();
        int seq = -1;
        int pos = start;

        while (pos + PACKET_SIZE <= data.length) {
            int type = data[pos] & 0xFF;
            if (type != PACKET_TYPE_DATA) {
                break;
            }

            seq = data[pos + 1] & 0xFF;
            if (lastSeq >= 0) {
                int expectedSeq = (lastSeq + 1) & 0xFF;
                if (seq != expectedSeq) {
                    droppedPackets++;
                }
            }
            lastSeq = seq;

            for (int i = 0; i < SAMPLES_PER_PACKET; i++) {
                int offset = pos + DATA_OFFSET + i * 3;
                if (offset + 2 >= data.length) break;

                int ppgHigh = data[offset] & 0xFF;
                int mid = data[offset + 1] & 0xFF;
                int ecgLow = data[offset + 2] & 0xFF;

                int ppg = (ppgHigh << 4) | (mid >> 4);
                int ecg = ((mid & 0x0F) << 8) | ecgLow;

                samples.add(new SampleData(ppg, ecg));
            }

            pos += PACKET_SIZE;
        }

        if (samples.isEmpty()) {
            return null;
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
