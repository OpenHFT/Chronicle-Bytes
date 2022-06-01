/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringRWPerfTest extends BytesTestCommon {

    public static final String UTF8 = "0123456789£123456789€123456789";
    public static final String ASCII = "012345678901234567890123456789";

    @Test
    public void test8bit() {
        final NativeBytes<Void> bytes = Bytes.allocateElasticDirect(32);
        final String s0 = ASCII;
        bytes.write8bit(s0);
        String s = bytes.read8bit();
        assertEquals(s0, s);
        bytes.releaseLast();
    }

    @Test
    public void testUtf8() {
        final NativeBytes<Void> bytes = Bytes.allocateElasticDirect(40);
        final String s0 = UTF8;
        bytes.writeUtf8(s0);
        String s = bytes.readUtf8();
        assertEquals(s0, s);
        bytes.releaseLast();
    }

    @Test
    public void testOnHeapPerf() {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap(40);
        doTestPerf(bytes);
    }

    @Test
    public void testDirectPerf() {
        final Bytes<?> bytes = Bytes.allocateElasticDirect(40);
        doTestPerf(bytes);
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
                assertEquals(ASCII, s);
                long end = System.nanoTime();
                timeAscii += end - start;
                bytes.clear();
                long start2 = System.nanoTime();
                bytes.writeUtf8(ASCII);
                String s2 = bytes.readUtf8();
                assertEquals(ASCII, s2);
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
