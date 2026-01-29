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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parameterised tests for {@link NativeBytes}, covering allocation sizing,
 * bounds checks, reference counting, and interactions with mapped files.
 */
@DisplayName("Native bytes allocation and resize behaviour")
public class NativeBytesTest extends BytesTestCommon {

    static Stream<Allocator> allocations() {
        return Stream.of(NATIVE, NATIVE_ADDRESS, HEAP, BYTE_BUFFER);
    }

    @ParameterizedTest
    @MethodSource("allocations")
    @DisplayName("write bytes triggers resize when needed")
    public void testWriteBytesWhereResizeNeeded0(Allocator alloc)
            throws IORuntimeException, BufferUnderflowException, BufferOverflowException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory required for resize needed zero test");
        Bytes<?> b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit(),
                "Read limit starts at buffer start for resize zero test");
        assertEquals(b.capacity(), b.writeLimit(),
                "Write limit equals capacity for resize zero test");
        assertEquals(1, b.realCapacity(),
                "Real capacity stays one for resize zero test");
        assertTrue(b.readLimit() < b.writeLimit(),
                "Read limit stays below write limit for resize zero test");

        Bytes<byte[]> wrap0 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.write(wrap0);
        assertEquals("Hello World, Have a great day!", b.toString(),
                "Greeting text round trips for resize zero test");
        b.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("allocations")
    @DisplayName("write bytes resizes for larger greeting")
    public void testWriteBytesWhereResizeNeeded(Allocator alloc)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException, BufferOverflowException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory required for resize needed test");
        Bytes<?> b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit(),
                "Read limit starts at buffer start for resize test");
        assertEquals(b.capacity(), b.writeLimit(),
                "Write limit equals capacity for resize test");
        assertEquals(1, b.realCapacity(),
                "Real capacity stays one for resize test");
        assertTrue(b.readLimit() < b.writeLimit(),
                "Read limit remains below write limit for resize test");

        Bytes<byte[]> wrap1 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.write(wrap1);
        assertEquals("Hello World, Have a great day!", b.toString(),
                "Greeting text round trips for resize test");
        b.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("allocations")
    @DisplayName("append non ascii char array round trips")
    public void testAppendCharArrayNonAscii(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory required for non ASCII append test");
        Bytes<?> b = alloc.elasticBytes(4);
        b.appendUtf8('\u0394');
        final byte[] bytes = "\u0394".getBytes(StandardCharsets.UTF_8);
        assertEquals(Bytes.wrapForRead(bytes).toHexString(), b.toHexString(),
                "UTF8 hex dump matches delta bytes after append");

        StringBuilder sb = new StringBuilder();
        b.parseUtf8(sb, 2);
        assertEquals("\u0394", sb.toString(),
                "Parsed UTF8 returns delta after two byte parse");

        b.readPosition(0);
        b.parseUtf8(sb, false, 1);
        assertEquals("\u0394", sb.toString(),
                "Parsed UTF8 returns delta after length one parse");

        b.clear();
        b.appendUtf8(new char[]{'\u0394'}, 0, 1);
        b.parseUtf8(sb, 2);
        assertEquals("\u0394", sb.toString(),
                "Parsed UTF8 returns delta after char array append");

        b.readPosition(0);
        assertEquals(new String(bytes, ISO_8859_1), b.toString(),
                "ISO8859 text view matches bytes string content");
        b.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("allocations")
    @DisplayName("append non ascii fails for short length")
    public void testAppendCharArrayNonAsciiToShort(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory required for short UTF8 parse test");
        Bytes<?> b = alloc.elasticBytes(4);
        try {
            b.appendUtf8('\u0394');
            final byte[] bytes = "\u0394".getBytes(StandardCharsets.UTF_8);
            assertEquals(Bytes.wrapForRead(bytes).toHexString(), b.toHexString(),
                    "UTF8 hex dump matches delta bytes before short parse");

            StringBuilder sb = new StringBuilder();
            assertThrows(UTFDataFormatRuntimeException.class,
                    () -> b.parseUtf8(sb, 1),
                    "UTF8 parse should fail when length is short");
        } finally {
            b.releaseLast();
        }
    }

    @ParameterizedTest
    @MethodSource("allocations")
    @DisplayName("native bytes grow from two pages")
    public void testResizeTwoPagesToThreePages(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory required for page resize test");
        assumeFalse(alloc == HEAP,
                "Heap allocator does not support page resize test");

        long pageSize = OS.pageSize();
        @NotNull NativeBytes<Void> nativeBytes = NativeBytes.nativeBytes(2 * pageSize);
        assertEquals(2 * pageSize, nativeBytes.realCapacity(),
                "Native capacity starts at two pages");
        nativeBytes.writePosition(nativeBytes.realCapacity() - 3);
        nativeBytes.writeInt(0);
        assertEquals(4 * pageSize, nativeBytes.realCapacity(),
                "Native capacity grows to four pages after write");

        nativeBytes.releaseLast();
    }

    @ParameterizedTest
    @MethodSource("allocations")
    @DisplayName("heap byte buffer refuses growth beyond limit")
    public void tryGrowBeyondByteBufferCapacity(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory required for heap buffer growth test");
        assumeFalse(alloc == HEAP,
                "Heap allocator does not support heap buffer grow test");
        // Capacity check: ensure max memory allows over-capacity test
        long maxMemory = Runtime.getRuntime().maxMemory();
        assumeTrue(maxMemory >= Bytes.MAX_HEAP_CAPACITY * 3L / 2,
                "Max heap memory must allow over capacity test");

        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(Bytes.MAX_HEAP_CAPACITY);
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertFalse(byteBuffer.isDirect(),
                "Heap byte buffer should not be direct");

        // Trigger growing beyond ByteBuffer
        bytes.writePosition(bytes.realCapacity() - 1);
        assertThrows(DecoratedBufferOverflowException.class,
                () -> bytes.writeInt(0),
                "Growing beyond max heap capacity should overflow buffer");
    }

    @ParameterizedTest
    @MethodSource("allocations")
    @DisplayName("elastic byte buffer stops at max capacity")
    public void tryGrowBeyondCapacity(Allocator alloc) {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory required for max capacity growth test");
        final int maxCapacity = 1024;
        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128, maxCapacity);
        assertEquals(128, bytes.realCapacity(),
                "Initial real capacity equals requested minimum");
        assertEquals(maxCapacity, bytes.capacity(),
                "Configured capacity equals declared maximum");
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertTrue(byteBuffer.isDirect(),
                "Elastic byte buffer uses direct storage");

        // trigger resize
        bytes.write(new byte[256]);
        try {
            // Trigger growing beyond maxCapacity
            assertThrows(BufferOverflowException.class,
                    () -> bytes.write(new byte[maxCapacity]),
                    "Write beyond declared maximum capacity should overflow");
        } finally {
            bytes.releaseLast();
        }
    }
}
