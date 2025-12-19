/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringRWPerfTest extends BytesTestCommon {

    private static final String UTF8 = "0123456789£123456789€123456789";
    private static final String ASCII = "012345678901234567890123456789";
    private Bytes<?> bytes;

    @AfterEach
    @Override
    public void afterChecks() {
        if (bytes != null)
            bytes.releaseLast();
        super.afterChecks();
    }

    @Test
    public void test8bit() {
        final NativeBytes<Void> bytes = Bytes.allocateElasticDirect(32);
        final String s0 = ASCII;
        bytes.write8bit(s0);
        String s = bytes.read8bit();
        assertEquals(s0, s, "write8bit/read8bit round-trip should preserve ASCII string");
        bytes.releaseLast();
    }

    @Test
    public void testUtf8() {
        final NativeBytes<Void> bytes = Bytes.allocateElasticDirect(40);
        final String s0 = UTF8;
        bytes.writeUtf8(s0);
        String s = bytes.readUtf8();
        assertEquals(s0, s, "writeUtf8/readUtf8 round-trip should preserve UTF-8 string");
        bytes.releaseLast();
    }

    @Test
    public void testOnHeapPerf() {
        bytes = Bytes.allocateElasticOnHeap(40);
        doTestPerf(bytes);
        assertEquals(0, bytes.readRemaining(), "testOnHeapPerf: bytes consumed");
    }

    @Test
    public void testDirectPerf() {
        bytes = Bytes.allocateElasticDirect(40);
        doTestPerf(bytes);
        assertEquals(0, bytes.readRemaining(), "testDirectPerf: bytes consumed");
    }

    private void doTestPerf(Bytes<?> bytes) {
        for (int t = 0; t < 4; t++) {
            long timeAscii = 0;
            long timeUtf = 0;
            final int runs = 50_000;
            for (int i = 0; i < runs; i++) {
                bytes.clear();
                long start = System.nanoTime();
                bytes.write8bit(ASCII);
                String s = bytes.read8bit();
                assertEquals(ASCII, s, "write8bit/read8bit performance test should preserve value");
                long end = System.nanoTime();
                timeAscii += end - start;
                bytes.clear();
                long start2 = System.nanoTime();
                bytes.writeUtf8(ASCII);
                String s2 = bytes.readUtf8();
                assertEquals(ASCII, s2, "writeUtf8/readUtf8 performance test should preserve value");
                long end2 = System.nanoTime();
                timeUtf += end2 - start2;
            }
            Thread.yield();
            timeAscii /= runs;
            timeUtf /= runs;
            if (t > 0)
                if (timeUtf > timeAscii)
                    System.err.println("timeUtf: " + timeUtf + " > timeAscii: " + timeAscii);
        }
    }
}
