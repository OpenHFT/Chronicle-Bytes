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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.HEAP;
import static net.openhft.chronicle.bytes.Allocator.NATIVE;
import static org.junit.Assert.*;

/*
 * Created by daniel on 17/04/15.
 */
@RunWith(Parameterized.class)
public class NativeBytesTest {

    private Allocator alloc;
    private ThreadDump threadDump;
    public NativeBytesTest(Allocator alloc) {
        this.alloc = alloc;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {NATIVE}, {HEAP}
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
        b.release();
    }

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
        b.release();
    }

    @Test
    public void testAppendCharArrayNonAscii() {
        Bytes b = alloc.elasticBytes(1);
        b.appendUtf8(new char[] {'Δ'}, 0, 1);
        b.release();
    }

    @Test
    public void testResizeTwoPagesToThreePages() {
        long pageSize = OS.pageSize();
        @NotNull NativeBytes<Void> nativeBytes = NativeBytes.nativeBytes(2 * pageSize);
        assertEquals(2 * pageSize, nativeBytes.realCapacity());
        nativeBytes.writePosition(nativeBytes.realCapacity() - 3);
        nativeBytes.writeInt(0);
        assertEquals(3 * pageSize, nativeBytes.realCapacity());

        nativeBytes.release();
    }

    //@Test
    //@Ignore("Long running test")
    public void tryGrowBeyondByteBufferCapacity() {
        if (Runtime.getRuntime().totalMemory() < Integer.MAX_VALUE)
            return;
        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(Bytes.MAX_BYTE_BUFFER_CAPACITY);
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertFalse(byteBuffer.isDirect());

        // Trigger growing beyond ByteBuffer
        bytes.writePosition(bytes.realCapacity() - 1);
        bytes.writeInt(0);

        assertTrue(bytes.realCapacity() > Integer.MAX_VALUE);
        assertNull(bytes.underlyingObject());

        // Check this is not a dream
        bytes.writeInt(Integer.MAX_VALUE + 100L, 42);
        assertEquals(42, bytes.readInt(Integer.MAX_VALUE + 100L));
    }
}