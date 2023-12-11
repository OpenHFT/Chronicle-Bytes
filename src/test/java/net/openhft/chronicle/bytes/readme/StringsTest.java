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
package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class StringsTest extends BytesTestCommon {

    @Test
    public void testString() {
        assumeFalse(NativeBytes.areNewGuarded());

        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.writeHexDumpDescription("write8bit").write8bit("£ 1");
            bytes.writeHexDumpDescription("writeUtf8").writeUtf8("£ 1");
            bytes.writeHexDumpDescription("append8bit").append8bit("£ 1").append('\n');
            bytes.writeHexDumpDescription("appendUtf8").appendUtf8("£ 1").append('\n');

            // System.out.println(bytes.toHexString());

            final String a = bytes.read8bit();
            final String b = bytes.readUtf8();
            final String c = bytes.parse8bit(StopCharTesters.CONTROL_STOP);
            final String d = bytes.parseUtf8(StopCharTesters.CONTROL_STOP);
            assertEquals("£ 1", a);
            assertEquals("£ 1", b);
            assertEquals("£ 1", c);
            assertEquals("£ 1", d);

            // System.out.println(System.identityHashCode(a));
            // System.out.println(System.identityHashCode(b));
            // System.out.println(System.identityHashCode(c));
            // System.out.println(System.identityHashCode(d));

            // uses the pool but a different hash.
            // assertSame(a, c); // uses a string pool
            assertSame(b, c); // uses a string pool
            assertSame(b, d); // uses a string pool
        } finally {
            bytes.releaseLast();
        }

    }

    @Test
    public void testNull() {
        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.writeHexDumpDescription("write8bit").write8bit((String) null);
            bytes.writeHexDumpDescription("writeUtf8").writeUtf8(null);

            //System.out.println(bytes.toHexString());

            final String a = bytes.read8bit();
            final String b = bytes.readUtf8();
            assertNull(a);
            assertNull(b);
        } finally {
            bytes.releaseLast();
        }
    }
}
