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
        // 6 bytes: 02 00 80 00 00 02 (first packet + second packet type marker)
        byte[] data = new byte[]{
                0x02, 0x00,
                (byte) 0x80, (byte) 0x00, (byte) 0x00,
                0x02
        };

        BleDataParser.ParseResult result = parser.parse(data);

        assertNotNull(result);
        assertEquals(1, result.samples.size());
        assertEquals(0, result.seq);
    }

    @Test
    public void parse_stripsHeader682F_parsesFrom02() {
        // 68 2F 02 00 80 00 00 02 (header + one packet + next type marker)
        byte[] data = new byte[]{
                (byte) 0x68, (byte) 0x2F,
                0x02, 0x00,
                (byte) 0x80, 0x00, 0x00,
                0x02
        };

        BleDataParser.ParseResult result = parser.parse(data);

        assertNotNull(result);
        assertEquals(1, result.samples.size());
        assertEquals(0, result.seq);
    }

    @Test
    public void parse_multiplePacketsInOneChunk_returnsAllSamples() {
        // 68 2F 02 00 80 00 00 02 01 40 02 00
        byte[] data = new byte[]{
                (byte) 0x68, (byte) 0x2F,
                0x02, 0x00,
                (byte) 0x80, 0x00, 0x00,
                0x02, 0x01,
                (byte) 0x40, 0x02, 0x00
        };

        BleDataParser.ParseResult result = parser.parse(data);

        assertNotNull(result);
        assertEquals(2, result.samples.size());
        assertEquals(1, result.seq);
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
        // 02 00 80 04 00 02 (one packet + next type marker)
        byte[] data = new byte[]{
                0x02, 0x00,
                (byte) 0x80, (byte) 0x04, (byte) 0x00,
                0x02
        };

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
        // 02 seq 00 00 00 02 (packet + next type marker for boundary validation)
        byte[] data = new byte[6];
        data[0] = 0x02;
        data[1] = (byte) seq;
        data[5] = 0x02;
        return data;
    }
}
