/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("Bytes copyTo buffer operations for heap and direct memory")
public class CopyToTest {

    @Test
    @DisplayName("copy from direct bytes into direct buffer")
    public void testCopyFromDirectBytesIntoByteBuffer() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for direct copy");

        Bytes<?> bytesToTest = Bytes.fromDirect("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocateDirect(128);
        copyToDestination.limit((int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String(),
                "Direct bytes copy preserves UTF-8 content");
    }

    @Test
    @DisplayName("copy from heap bytes into heap buffer")
    public void testCopyFromHeapBytesIntoByteBuffer() {
        Bytes<?> bytesToTest = Bytes.from("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocate(128);
        copyToDestination.limit((int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String(),
                "Heap bytes copy preserves UTF-8 content");
    }
}
