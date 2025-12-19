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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Allocator.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Parameterised tests for {@link NativeBytes}, covering allocation sizing,
 * bounds checks, reference counting, and interactions with mapped files.
 */
public class NativeBytesTest extends BytesTestCommon {

    private Allocator alloc;

    public void initNativeBytesTest(Allocator alloc) {
        this.alloc = alloc;
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {NATIVE}, {NATIVE_ADDRESS}, {HEAP}, {BYTE_BUFFER}
        });
    }

    @BeforeEach
    public void hasDirectMemory() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testWriteBytesWhereResizeNeeded0(Allocator alloc)
            throws IORuntimeException, BufferUnderflowException, BufferOverflowException {
        initNativeBytesTest(alloc);
        Bytes<?> b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit(), "read limit should equal start position for new elastic Bytes");
        assertEquals(b.capacity(), b.writeLimit(), "write limit should equal capacity for new elastic Bytes");
        assertEquals(1, b.realCapacity(), "real capacity should be 1 byte as requested in allocation");
        assertTrue(b.readLimit() < b.writeLimit(), "read limit should be less than write limit for new elastic Bytes");

        Bytes<byte[]> wrap0 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.write(wrap0);
        assertEquals("Hello World, Have a great day!", b.toString(), "written content should match original string after elastic resize");
        b.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testWriteBytesWhereResizeNeeded(Allocator alloc)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException, BufferOverflowException {
        initNativeBytesTest(alloc);
        Bytes<?> b = alloc.elasticBytes(1);
        assertEquals(b.start(), b.readLimit(), "read limit should equal start position for new elastic Bytes");
        assertEquals(b.capacity(), b.writeLimit(), "write limit should equal capacity for new elastic Bytes");
        assertEquals(1, b.realCapacity(), "real capacity should be 1 byte as requested in allocation");
        assertTrue(b.readLimit() < b.writeLimit(), "read limit should be less than write limit for new elastic Bytes");

        Bytes<byte[]> wrap1 = Bytes.wrapForRead("Hello World, Have a great day!".getBytes(ISO_8859_1));
        b.write(wrap1);
        assertEquals("Hello World, Have a great day!", b.toString(), "written content should match original string after elastic resize");
        b.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testAppendCharArrayNonAscii(Allocator alloc) {
        initNativeBytesTest(alloc);
        Bytes<?> b = alloc.elasticBytes(4);
        b.appendUtf8('\u0394');
        final byte[] bytes = "\u0394".getBytes(StandardCharsets.UTF_8);
        assertEquals(Bytes.wrapForRead(bytes).toHexString(), b.toHexString(), "Bytes.wrapForRead");

        StringBuilder sb = new StringBuilder();
        b.parseUtf8(sb, 2);
        assertEquals("\u0394", sb.toString(), "parsed UTF-8 content should match original delta character");

        b.readPosition(0);
        b.parseUtf8(sb, false, 1);
        assertEquals("\u0394", sb.toString(), "parsed UTF-8 content in append-only mode should match original delta character");

        b.clear();
        b.appendUtf8(new char[]{'\u0394'}, 0, 1);
        b.parseUtf8(sb, 2);
        assertEquals("\u0394", sb.toString(), "parsed UTF-8 content after appendUtf8 should match original delta character");

        b.readPosition(0);
        assertEquals(new String(bytes, ISO_8859_1), b.toString(), "Bytes toString should match ISO-8859-1 representation of UTF-8 encoded delta");
        b.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testAppendCharArrayNonAsciiToShort(Allocator alloc) {
        initNativeBytesTest(alloc);
        Bytes<?> b = alloc.elasticBytes(4);
        try {
            b.appendUtf8('\u0394');
            final byte[] bytes = "\u0394".getBytes(StandardCharsets.UTF_8);
            assertEquals(Bytes.wrapForRead(bytes).toHexString(), b.toHexString(), "Bytes.wrapForRead");

            StringBuilder sb = new StringBuilder();
            assertThrows(UTFDataFormatRuntimeException.class, () ->
                    b.parseUtf8(sb, 1)
            );
        } finally {
            b.releaseLast();
        }
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testResizeTwoPagesToThreePages(Allocator alloc) {
        initNativeBytesTest(alloc);
        assumeFalse(alloc == HEAP);

        long pageSize = OS.pageSize();
        @NotNull NativeBytes<Void> nativeBytes = NativeBytes.nativeBytes(2 * pageSize);
        assertEquals(2 * pageSize, nativeBytes.realCapacity(), "real capacity should be 2 pages as initially allocated");
        nativeBytes.writePosition(nativeBytes.realCapacity() - 3);
        nativeBytes.writeInt(0);
        assertEquals(4 * pageSize, nativeBytes.realCapacity(), "real capacity should resize to 4 pages after writing beyond boundary");

        nativeBytes.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest
    public void tryGrowBeyondByteBufferCapacity(Allocator alloc) {
        initNativeBytesTest(alloc);
        assumeFalse(alloc == HEAP);
        long maxMemory = Runtime.getRuntime().maxMemory();
        Assumptions.assumeTrue(maxMemory >= Bytes.MAX_HEAP_CAPACITY * 3L / 2);

        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(Bytes.MAX_HEAP_CAPACITY);
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertFalse(byteBuffer.isDirect(), "byteBuffer.isDirect");

        // Trigger growing beyond ByteBuffer
        bytes.writePosition(bytes.realCapacity() - 1);
        assertThrows(DecoratedBufferOverflowException.class, () ->
                bytes.writeInt(0)
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    public void tryGrowBeyondCapacity(Allocator alloc) {
        initNativeBytesTest(alloc);
        final int maxCapacity = 1024;
        @NotNull Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128, maxCapacity);
        assertEquals(128, bytes.realCapacity(), "real capacity should be 128 bytes as initially allocated");
        assertEquals(maxCapacity, bytes.capacity(), "capacity should be 1024 bytes as specified max capacity");
        @Nullable ByteBuffer byteBuffer = bytes.underlyingObject();
        assertTrue(byteBuffer.isDirect(), "byteBuffer.isDirect");

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
