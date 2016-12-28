/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class BytesInternalTest {

    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }
    @Test
    public void testParseUTF_SB1() throws UTFDataFormatRuntimeException {
        @NotNull VanillaBytes bytes = Bytes.allocateElasticDirect();
        @NotNull byte[] bytes2 = new byte[128];
        Arrays.fill(bytes2, (byte) '?');
        bytes.write(bytes2);

        @NotNull StringBuilder sb = new StringBuilder();

        BytesInternal.parseUtf8(bytes, sb, 128);
        assertEquals(128, sb.length());
        assertEquals(new String(bytes2, 0), sb.toString());
    }

    @Test
    public void testCompareUTF() throws IORuntimeException {
        @NotNull NativeBytesStore<Void> bs = NativeBytesStore.nativeStore(32);
        bs.writeUtf8(0, "test");
        assertTrue(BytesInternal.compareUtf8(bs, 0, "test"));
        assertFalse(BytesInternal.compareUtf8(bs, 0, null));

        bs.writeUtf8(0, null);
        assertTrue(BytesInternal.compareUtf8(bs, 0, null));
        assertFalse(BytesInternal.compareUtf8(bs, 0, "test"));

        bs.writeUtf8(1, "£€");
        @NotNull StringBuilder sb = new StringBuilder();
        bs.readUtf8(1, sb);
        assertEquals("£€", sb.toString());
        assertTrue(BytesInternal.compareUtf8(bs, 1, "£€"));
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£"));
        assertFalse(BytesInternal.compareUtf8(bs, 1, "£€$"));
    }

    @Test
    public void testParseDouble() {
        @NotNull Object[][] tests = {
                {"-1E-3 ", -1E-3},
                {"12E3 ", 12E3},
                {"-1.1E-3 ", -1.1E-3},
                {"-1.1E3 ", -1.1E3},
                {"-1.16823E70 ", -1.16823E70},
                {"1.17045E70 ", 1.17045E70}
        };
        for (Object[] objects : tests) {
            @NotNull String text = (String) objects[0];
            double expected = (Double) objects[1];

            assertEquals(expected, Bytes.from(text)
                    .parseDouble(), 0.0);
        }
    }
}