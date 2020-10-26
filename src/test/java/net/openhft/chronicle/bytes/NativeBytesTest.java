/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class NativeBytesTest extends BytesTestCommon {

    private Allocator alloc;
    private ThreadDump threadDump;

    public NativeBytesTest(Allocator alloc) {
        this.alloc = alloc;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {NATIVE}, {HEAP}, {BYTE_BUFFER}
        });
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testWriteBytesWhereResizeNeeded0() throws IORuntimeException, BufferUnderflowException, BufferOverflowException {
        Bytes b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit());
        assertEquals(b.capacity(), b.writeLimit());
        assertEquals(1, b.realCapacity());
        assertTrue(b.readLimit() < b.writeLimit());

        Bytes<byte[]> wrap0 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.writeSome(wrap0);
        assertEquals("Hello World, Have a great day!", b.toString());
        b.releaseLast();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testWriteBytesWhereResizeNeeded() throws IllegalArgumentException, IORuntimeException, BufferUnderflowException, BufferOverflowException {
        Bytes b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit());
        assertEquals(b.capacity(), b.writeLimit());
        assertEquals(1, b.realCapacity());
        assertTrue(b.readLimit() < b.writeLimit());

        Bytes<byte[]> wrap1 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.writeSome(wrap1);
        assertEquals("Hello World, Have a great day!", b.toString());
        b.releaseLast();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testAppendCharArrayNonAscii() {
        Bytes b = alloc.elasticBytes(1);
        b.appendUtf8(new char[]{'Δ'}, 0, 1);
        b.releaseLast();
    }

    @Test
    public void testResizeTwoPagesToThreePages() {
        Assume.assumeFalse(alloc == HEAP);

        long pageSize = OS.pageSize();
        @NotNull NativeBytes<Void> nativeBytes = NativeBytes.nativeBytes(2 * pageSize);
        assertEquals(2 * pageSize, nativeBytes.realCapacity());
        nativeBytes.writePosition(nativeBytes.realCapacity() - 3);
        nativeBytes.writeInt(0);
        assertEquals(4 * pageSize, nativeBytes.realCapacity());

        nativeBytes.releaseLast();
    }

    @Test(expected = DecoratedBufferOverflowException.class)
    public void tryGrowBeyondByteBufferCapacity() {
        Assume.assumeFalse(alloc == HEAP);
        Assume.assumeFalse(Jvm.isArm());

        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory < Integer.MAX_VALUE && !OS.isLinux())
            return;
        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(Bytes.MAX_HEAP_CAPACITY);
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertFalse(byteBuffer.isDirect());

        // Trigger growing beyond ByteBuffer
        bytes.writePosition(bytes.realCapacity() - 1);
        bytes.writeInt(0);
    }

    @Test(expected = BufferOverflowException.class)
    public void tryGrowBeyondCapacity() {
        final int maxCapacity = 1024;
        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128, maxCapacity);
        assertEquals(128, bytes.realCapacity());
        assertEquals(maxCapacity, bytes.capacity());
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertTrue(byteBuffer.isDirect());

        // trigger resize
        bytes.write(new byte[256]);
        try {
            // Trigger growing beyond maxCapacity
            bytes.write(new byte[maxCapacity]);
            Assert.fail("should not get here");
        } finally {
            bytes.releaseLast();
        }
    }
}
