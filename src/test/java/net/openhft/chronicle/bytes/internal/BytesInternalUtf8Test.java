/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.bytes.StreamingDataOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BytesInternal UTF8 append and parse helpers")
public class BytesInternalUtf8Test extends BytesTestCommon {

    @Test
    @DisplayName("append UTF8 variants into bytes output")
    public void appendUtf8CharSequenceVariants() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(32);
        try {
            CharSequence cs = "hello-world";
            BytesInternal.appendUtf8((StreamingDataOutput) out, cs, 0, cs.length());
            assertEquals("hello-world", out.toString(),
                    "UTF8 append writes hello-world");

            out.clear();
            char[] chars = "abcdef".toCharArray();
            // use end index exclusive one less to avoid inclusive access
            BytesInternal.appendUtf8(out, (CharSequence) new String(chars), 1, chars.length - 1);
            assertEquals("bcdef", out.toString(),
                    "UTF8 append writes selected substring");

            // long string across internal buffers
            out.clear();
            String longStr = new String(new char[1024]).replace('\0', 'x');
            BytesInternal.appendUtf8(out, longStr, 0, longStr.length());
            assertEquals(longStr.length(), out.length(),
                    "UTF8 append writes full long string length");
        } finally {
            out.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 and 8bit using stop testers")
    public void parseUtf8And8bitWithStopTesters() {
        Bytes<?> a = Bytes.from("alpha");
        Bytes<?> b = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(a, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString(),
                    "UTF8 parse reads alpha");

            sb.setLength(0);
            BytesInternal.parseUtf8(b, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("beta", sb.toString(),
                    "UTF8 parse reads beta");
        } finally {
            a.releaseLast();
            b.releaseLast();
        }
    }
}
