/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.bytes.internal.UnsafeText;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class UnsafeTextBytesTest extends BytesTestCommon {

    static void testAppendBase10(final Bytes<?> bytes, final long l) {
        final long address = bytes.clear().addressForRead(0);
        final long end = UnsafeText.appendFixed(address, l);
        bytes.readLimit(end - address);
        String message = bytes.toString();
        assertEquals(message, l, bytes.parseLong());
    }

    static String testAppendDouble(final Bytes<?> bytes, final double l) {
        final long address = bytes.clear().addressForRead(0);
        final long end = UnsafeText.appendDouble(address, l);
        bytes.readLimit(end - address);
        final String message = bytes.toString();
        assertEquals(message, l, bytes.parseDouble(), Math.ulp(l));
        return message;
    }

    static void testAppendFixed(final Bytes<?> bytes,
                                final double l,
                                final int digits) {
        final long address = bytes.clear().addressForRead(0);
        final long end = UnsafeText.appendFixed(address, l, digits);
        bytes.readLimit(end - address);
        final String message = bytes.toString();
        final double expected = Maths.round4(l);
        final double actual = bytes.parseDouble();
        assertEquals(message, expected, actual, 0.0);
    }

    @Test
    public void appendBase10() {
        final Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            for (long l = Long.MAX_VALUE; l > 0; l /= 2) {
                testAppendBase10(bytes, l);
                testAppendBase10(bytes, 1 - l);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void appendDouble() {

        final Bytes<?> bytes = Bytes.allocateDirect(32);
        // testAppendFixed(bytes, 864960913420.1180, 4);
        testAppendFixed(bytes, 98472368148.9340, 4);
        testAppendFixed(bytes, 21.0607, 4);

        final Random rand = new Random(1);
        try {
            testAppendFixed(bytes, 0.0003, 4);
            for (int i = 0; i < 300000; i++) {
                double d = Math.pow(1e15, rand.nextDouble()) / 1e4;
                testAppendDouble(bytes, d);
                testAppendFixed(bytes, d, 4);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void appendDouble2() {
        final Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            for (double d : new double[]{
                    741138311171.555,
                    0.0, -0.0, 0.1, 0.012, 0.00123, 1.0, Double.NaN, 1 / 0.0, -1 / 0.0})
                testAppendDouble(bytes, d);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void extraZeros() {
        final Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            final double d = -0.00002;
            final String output = testAppendDouble(bytes, d);
            assertEquals("-0.00002", output);
        } finally {
            bytes.releaseLast();
        }
    }
}
