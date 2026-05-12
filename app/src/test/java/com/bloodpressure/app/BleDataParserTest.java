package com.bloodpressure.app;

import com.bloodpressure.app.ble.BleDataParser;
import com.bloodpressure.app.data.model.SampleData;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BleDataParserTest {

    private BleDataParser parser;

    @Before
    public void setUp() {
        parser = new BleDataParser();
    }

    @Test
    public void parse_validDataPacket_returns1Sample() {
        // 5 bytes: 1 type + 1 seq + 3 data (1 sample * 3 bytes)
        byte[] data = new byte[5];
        data[0] = 0x02; // type
        data[1] = 0x00; // seq
        data[2] = (byte) 0x80; // PPG high
        data[3] = (byte) 0x00; // mid
        data[4] = (byte) 0x00; // ECG low

        BleDataParser.ParseResult result = parser.parse(data);

        assertNotNull(result);
        assertEquals(1, result.samples.size());
        assertEquals(0, result.seq);
    }

    @Test
    public void parse_invalidType_returnsNull() {
        byte[] data = new byte[5];
        data[0] = 0x01; // wrong type

        BleDataParser.ParseResult result = parser.parse(data);
        assertNull(result);
    }

    @Test
    public void parse_shortData_returnsNull() {
        byte[] data = new byte[4];
        BleDataParser.ParseResult result = parser.parse(data);
        assertNull(result);
    }

    @Test
    public void parse_nullData_returnsNull() {
        BleDataParser.ParseResult result = parser.parse(null);
        assertNull(result);
    }

    @Test
    public void parse_correctPpgEcgValues() {
        byte[] data = new byte[5];
        data[0] = 0x02;
        data[1] = 0x00;

        // Sample: PPG=0x800, ECG=0x400
        // PPG[11:4] = 0x80, PPG[3:0]<<4|ECG[11:8] = 0x04, ECG[7:0] = 0x00
        data[2] = (byte) 0x80;
        data[3] = (byte) 0x04;
        data[4] = (byte) 0x00;

        BleDataParser.ParseResult result = parser.parse(data);
        assertNotNull(result);

        SampleData first = result.samples.get(0);
        assertEquals(0x800, first.getPpg()); // 2048
        assertEquals(0x400, first.getEcg()); // 1024
    }

    @Test
    public void parse_seqTracking_detectsDrops() {
        // First packet seq=0
        byte[] data1 = createPacket(0x00);
        parser.parse(data1);

        // Second packet seq=2 (skip seq=1)
        byte[] data2 = createPacket(0x02);
        BleDataParser.ParseResult result = parser.parse(data2);

        assertEquals(1, result.totalDropped);
    }

    @Test
    public void reset_clearsState() {
        byte[] data = createPacket(0x00);
        parser.parse(data);
        parser.reset();
        assertEquals(0, parser.getDroppedPackets());
    }

    private byte[] createPacket(int seq) {
        byte[] data = new byte[5];
        data[0] = 0x02;
        data[1] = (byte) seq;
        return data;
    }
}
