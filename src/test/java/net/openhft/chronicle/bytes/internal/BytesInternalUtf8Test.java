/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.bytes.StreamingDataOutput;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesInternalUtf8Test extends BytesTestCommon {

    @Test
    public void appendUtf8CharSequenceVariants() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(32);
        try {
            CharSequence cs = "hello-world";
            BytesInternal.appendUtf8((StreamingDataOutput) out, cs, 0, cs.length());
            assertEquals("hello-world", out.toString());

            out.clear();
            char[] chars = "abcdef".toCharArray();
            // use end index exclusive one less to avoid inclusive access
            BytesInternal.appendUtf8(out, (CharSequence) new String(chars), 1, chars.length - 1);
            assertEquals("bcdef", out.toString());

            // long string across internal buffers
            out.clear();
            String longStr = new String(new char[1024]).replace('\0', 'x');
            BytesInternal.appendUtf8(out, longStr, 0, longStr.length());
            assertEquals(longStr.length(), out.length());
        } finally {
            out.releaseLast();
        }
    }

    @Test
    public void parseUtf8And8bitWithStopTesters() {
        Bytes<?> a = Bytes.from("alpha");
        Bytes<?> b = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(a, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString());

            sb.setLength(0);
            BytesInternal.parseUtf8(b, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("beta", sb.toString());
        } finally {
            a.releaseLast();
            b.releaseLast();
        }
    }
}
