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

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by daniel on 17/04/15.
 */
public class NativeBytesTest {

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
    public void testWriteBytesWhereResizeNeeded0() throws IORuntimeException, BufferUnderflowException, BufferOverflowException {
        Bytes b = Bytes.allocateElasticDirect();
        assertEquals(b.start(), b.readLimit());
        assertEquals(b.capacity(), b.writeLimit());
        assertEquals(0, b.realCapacity());
        assertTrue(b.readLimit() < b.writeLimit());

        Bytes<byte[]> wrap0 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.writeSome(wrap0);
        assertEquals("Hello World, Have a great day!", b.toString());
    }

    @Test
    public void testWriteBytesWhereResizeNeeded() throws IllegalArgumentException, IORuntimeException, BufferUnderflowException, BufferOverflowException {
        Bytes b = Bytes.allocateElasticDirect(1);
        assertEquals(b.start(), b.readLimit());
        assertEquals(b.capacity(), b.writeLimit());
        assertEquals(1, b.realCapacity());
        assertTrue(b.readLimit() < b.writeLimit());

        Bytes<byte[]> wrap1 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.writeSome(wrap1);
        assertEquals("Hello World, Have a great day!", b.toString());
    }

    @Test
    public void testAppendCharArrayNonAscii() {
        Bytes b = Bytes.allocateElasticDirect();
        b.appendUtf8(new char[] {'Δ'}, 0, 1);
    }

    @Test
    public void testResizeTwoPagesToThreePages() {
        long pageSize = OS.pageSize();
        NativeBytes<Void> nativeBytes = NativeBytes.nativeBytes(2 * pageSize);
        assertEquals(2 * pageSize, nativeBytes.realCapacity());
        nativeBytes.writePosition(nativeBytes.realCapacity() - 3);
        nativeBytes.writeInt(0);
        assertEquals(3 * pageSize, nativeBytes.realCapacity());
    }
}