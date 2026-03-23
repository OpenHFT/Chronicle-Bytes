/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class NativeBytesTest extends BytesTestCommon {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(NATIVE),
                Arguments.of(NATIVE_ADDRESS),
                Arguments.of(HEAP),
                Arguments.of(BYTE_BUFFER)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testWriteBytesWhereResizeNeeded0(Allocator alloc)
            throws IORuntimeException, BufferUnderflowException, BufferOverflowException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<?> b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit());
        assertEquals(b.capacity(), b.writeLimit());
        assertEquals(1, b.realCapacity());
        assertTrue(b.readLimit() < b.writeLimit());

        Bytes<byte[]> wrap0 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.write(wrap0);
        assertEquals("Hello World, Have a great day!", b.toString());
        b.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testWriteBytesWhereResizeNeeded(Allocator alloc)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException, BufferOverflowException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<?> b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit());
        assertEquals(b.capacity(), b.writeLimit());
        assertEquals(1, b.realCapacity());
        assertTrue(b.readLimit() < b.writeLimit());

        Bytes<byte[]> wrap1 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.write(wrap1);
        assertEquals("Hello World, Have a great day!", b.toString());
        b.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testAppendCharArrayNonAscii(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<?> b = alloc.elasticBytes(4);
        b.appendUtf8('\u0394');
        final byte[] bytes = "\u0394".getBytes(StandardCharsets.UTF_8);
        assertEquals(Bytes.wrapForRead(bytes).toHexString(), b.toHexString());

        StringBuilder sb = new StringBuilder();
        b.parseUtf8(sb, 2);
        assertEquals("\u0394", sb.toString());

        b.readPosition(0);
        b.parseUtf8(sb, false, 1);
        assertEquals("\u0394", sb.toString());

        b.clear();
        b.appendUtf8(new char[]{'\u0394'}, 0, 1);
        b.parseUtf8(sb, 2);
        assertEquals("\u0394", sb.toString());

        b.readPosition(0);
        assertEquals(new String(bytes, ISO_8859_1), b.toString());
        b.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testAppendCharArrayNonAsciiToShort(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<?> b = alloc.elasticBytes(4);
        try {
            b.appendUtf8('\u0394');
            final byte[] bytes = "\u0394".getBytes(StandardCharsets.UTF_8);
            assertEquals(Bytes.wrapForRead(bytes).toHexString(), b.toHexString());

            StringBuilder sb = new StringBuilder();
            assertThrows(UTFDataFormatRuntimeException.class, () ->
                    b.parseUtf8(sb, 1)
            );
        } finally {
            b.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testResizeTwoPagesToThreePages(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        assumeFalse(alloc == HEAP);

        long pageSize = OS.pageSize();
        @NotNull NativeBytes<Void> nativeBytes = NativeBytes.nativeBytes(2 * pageSize);
        assertEquals(2 * pageSize, nativeBytes.realCapacity());
        nativeBytes.writePosition(nativeBytes.realCapacity() - 3);
        nativeBytes.writeInt(0);
        assertEquals(4 * pageSize, nativeBytes.realCapacity());

        nativeBytes.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void tryGrowBeyondByteBufferCapacity(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        assumeFalse(alloc == HEAP);
        long maxMemory = Runtime.getRuntime().maxMemory();
        assumeTrue(maxMemory >= Bytes.MAX_HEAP_CAPACITY * 3L / 2);

        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(Bytes.MAX_HEAP_CAPACITY);
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertFalse(byteBuffer.isDirect());

        // Trigger growing beyond ByteBuffer
        bytes.writePosition(bytes.realCapacity() - 1);
        assertThrows(DecoratedBufferOverflowException.class, () ->
                bytes.writeInt(0)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void tryGrowBeyondCapacity(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0);
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
            assertThrows(BufferOverflowException.class, () ->
                    bytes.write(new byte[maxCapacity])
            );
        } finally {
            bytes.releaseLast();
        }
    }
}
