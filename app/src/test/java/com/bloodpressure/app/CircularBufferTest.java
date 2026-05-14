package com.bloodpressure.app;

import com.bloodpressure.app.data.model.SampleData;
import com.bloodpressure.app.waveform.CircularBuffer;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CircularBufferTest {

    private CircularBuffer buffer;

    @Before
    public void setUp() {
        buffer = new CircularBuffer(100);
    }

    @Test
    public void add_singleItem_sizeIncreases() {
        buffer.add(100, 200);
        assertEquals(1, buffer.getSize());
    }

    @Test
    public void add_multipleItems_sizeIncreases() {
        for (int i = 0; i < 10; i++) {
            buffer.add(i, i * 2);
        }
        assertEquals(10, buffer.getSize());
    }

    @Test
    public void add_exceedsCapacity_overwritesOldest() {
        CircularBuffer smallBuffer = new CircularBuffer(5);
        for (int i = 0; i < 10; i++) {
            smallBuffer.add(i, i);
        }
        assertEquals(5, smallBuffer.getSize());
    }

    @Test
    public void getPpgRange_returnsCorrectData() {
        buffer.add(100, 200);
        buffer.add(300, 400);

        int[] ppg = new int[10];
        int len = buffer.copyPpgRange(ppg, 2);
        assertEquals(2, len);
        assertEquals(100, ppg[0]);
        assertEquals(300, ppg[1]);
    }

    @Test
    public void getEcgRange_returnsCorrectData() {
        buffer.add(100, 200);
        buffer.add(300, 400);

        int[] ecg = new int[10];
        int len = buffer.copyEcgRange(ecg, 2);
        assertEquals(2, len);
        assertEquals(200, ecg[0]);
        assertEquals(400, ecg[1]);
    }

    @Test
    public void getRange_countLargerThanSize_returnsAll() {
        buffer.add(100, 200);
        int[] ppg = new int[10];
        int len = buffer.copyPpgRange(ppg, 10);
        assertEquals(1, len);
    }

    @Test
    public void addBatch_addsMultipleSamples() {
        List<SampleData> samples = new ArrayList<>();
        samples.add(new SampleData(100, 200));
        samples.add(new SampleData(300, 400));
        samples.add(new SampleData(500, 600));

        buffer.addBatch(samples);
        assertEquals(3, buffer.getSize());

        int[] ppg = new int[10];
        int len = buffer.copyPpgRange(ppg, 3);
        assertEquals(3, len);
        assertEquals(100, ppg[0]);
        assertEquals(300, ppg[1]);
        assertEquals(500, ppg[2]);
    }

    @Test
    public void clear_resetsBuffer() {
        buffer.add(100, 200);
        buffer.add(300, 400);
        buffer.clear();
        assertEquals(0, buffer.getSize());
    }

    @Test
    public void getRange_emptyBuffer_returnsZeroLength() {
        int[] ppg = new int[10];
        int len = buffer.copyPpgRange(ppg, 10);
        assertEquals(0, len);
    }

    @Test
    public void threadSafety_concurrentReadWrite() throws InterruptedException {
        CircularBuffer sharedBuffer = new CircularBuffer(1000);

        Thread writer = new Thread(() -> {
            for (int i = 0; i < 5000; i++) {
                sharedBuffer.add(i, i);
            }
        });

        int[] readBuf = new int[100];
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 5000; i++) {
                sharedBuffer.copyPpgRange(readBuf, 100);
            }
        });

        writer.start();
        reader.start();
        writer.join(5000);
        reader.join(5000);

        // Should not throw any exceptions
        assertTrue(sharedBuffer.getSize() > 0);
    }
}
