/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.BufferOverflowException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests copy operations across direct and mapped memory regions because correct data
 * transfer is required to avoid corruption when crossing chunk boundaries in mapped files.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("Bytes copy operations across direct and mapped memory regions")
public class CopyBytesTest extends BytesTestCommon {

    private static void doTest(Bytes<?> toTest, int from) {
        Bytes<?> toCopy = Bytes.allocateDirect(32);
        Bytes<?> toValidate = Bytes.allocateDirect(32);
        try {
            toCopy.writeLong(0, (long) 'W' << 56L | 100L);
            toCopy.writeLong(8, (long) 'W' << 56L | 200L);

            toTest.writePosition(from);
            toTest.write(toCopy, 0, 2 * 8L);
            toTest.write(toCopy, 0, 8L);

            toTest.readPosition(from);
            toTest.read(toValidate, 3 * 8);

            assertEquals((long) 'W' << 56L | 100L, toValidate.readLong(0),
                    "First copied long matches expected marker");
            assertEquals((long) 'W' << 56L | 200L, toValidate.readLong(8),
                    "Second copied long matches expected marker");
            assertEquals((long) 'W' << 56L | 100L, toValidate.readLong(16),
                    "Third copied long repeats first marker");

        } finally {
            toTest.releaseLast();
            toCopy.releaseLast();
            toValidate.releaseLast();
            // close if closeable.
            Closeable.closeQuietly(toTest);
        }
    }

    @BeforeEach
    public void directEnabled() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for copy tests");
    }

    @Test
    @DisplayName("copy bytes from direct store into direct store")
    public void testCanCopyBytesFromBytes() {
        doTest(Bytes.allocateElasticDirect(), 0);
    }

    @Test
    @DisplayName("copy bytes from mapped bytes at start position")
    public void testCanCopyBytesFromMappedBytes1()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), 0);
    }

    @Test
    @DisplayName("copy bytes from single mapped bytes at start position")
    public void testCanCopyBytesFromMappedBytesSingle1()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.singleMappedBytes(bytes, 64 << 10), 0);
    }

    @Test
    @DisplayName("copy bytes from mapped bytes near page boundary")
    public void testCanCopyBytesFromMappedBytes2()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), (64 << 10) - 8);
    }

    @Test
    @DisplayName("copy bytes from single mapped bytes near boundary")
    public void testCanCopyBytesFromMappedBytesSingle2()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.singleMappedBytes(bytes, 128 << 10), (64 << 10) - 8);
    }

    @Test
    @DisplayName("copy bytes from mapped bytes with offset window")
    public void testCanCopyBytesFromMappedBytes3()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        doTest(MappedBytes.mappedBytes(bytes, 16 << 10, 16 << 10), (64 << 10) - 8);
    }

    @Test
    @DisplayName("copy bytes from small single map throws overflow")
    public void testCanCopyBytesFromMappedBytesSingle3()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        assertThrows(BufferOverflowException.class,
                () -> doTest(MappedBytes.singleMappedBytes(bytes, 32 << 10), (64 << 10) - 8),
                "Copying beyond single mapped bytes throws overflow");
    }
}
