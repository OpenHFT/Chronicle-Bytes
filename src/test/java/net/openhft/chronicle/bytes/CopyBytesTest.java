/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.BufferOverflowException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class CopyBytesTest extends BytesTestCommon {

    private static final long W100 = (long) 'W' << 56L | 100L;
    private static final long W200 = (long) 'W' << 56L | 200L;

    private static long[] doTest(Bytes<?> toTest, int from) {
        Bytes<?> toCopy = Bytes.allocateDirect(32);
        Bytes<?> toValidate = Bytes.allocateDirect(32);
        try {
            toCopy.writeLong(0, W100);
            toCopy.writeLong(8, W200);

            toTest.writePosition(from);
            toTest.write(toCopy, 0, 2 * 8L);
            toTest.write(toCopy, 0, 8L);

            toTest.readPosition(from);
            toTest.read(toValidate, 3 * 8);

            return new long[]{
                    toValidate.readLong(0),
                    toValidate.readLong(8),
                    toValidate.readLong(16)
            };
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
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    public void testCanCopyBytesFromBytes() {
        assertArrayEquals(new long[]{W100, W200, W100}, doTest(Bytes.allocateElasticDirect(), 0), "testCanCopyBytesFromBytes: copy");
    }

    @Test
    public void testCanCopyBytesFromMappedBytes1()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        assertArrayEquals(new long[]{W100, W200, W100}, doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), 0), "testCanCopyBytesFromMappedBytes1: copy");
    }

    @Test
    public void testCanCopyBytesFromMappedBytesSingle1()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        assertArrayEquals(new long[]{W100, W200, W100}, doTest(MappedBytes.singleMappedBytes(bytes, 64 << 10), 0), "testCanCopyBytesFromMappedBytesSingle1: copy");
    }

    @Test
    public void testCanCopyBytesFromMappedBytes2()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        assertArrayEquals(new long[]{W100, W200, W100}, doTest(MappedBytes.mappedBytes(bytes, 64 << 10, 0), (64 << 10) - 8), "testCanCopyBytesFromMappedBytes2: copy");
    }

    @Test
    public void testCanCopyBytesFromMappedBytesSingle2()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        assertArrayEquals(new long[]{W100, W200, W100}, doTest(MappedBytes.singleMappedBytes(bytes, 128 << 10), (64 << 10) - 8), "testCanCopyBytesFromMappedBytesSingle2: copy");
    }

    @Test
    public void testCanCopyBytesFromMappedBytes3()
            throws Exception {
        File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
        bytes.deleteOnExit();
        assertArrayEquals(new long[]{W100, W200, W100}, doTest(MappedBytes.mappedBytes(bytes, 16 << 10, 16 << 10), (64 << 10) - 8), "testCanCopyBytesFromMappedBytes3: copy");
    }

    @Test
    public void testCanCopyBytesFromMappedBytesSingle3()
            throws Exception {
        assertThrows(BufferOverflowException.class, () -> {
            File bytes = Files.createTempFile("mapped-test", "bytes").toFile();
            bytes.deleteOnExit();
            doTest(MappedBytes.singleMappedBytes(bytes, 32 << 10), (64 << 10) - 8);
        });
    }
}
