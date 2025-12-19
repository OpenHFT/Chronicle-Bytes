/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.BufferUtil;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class CopyToTest {

    @Test
    public void testCopyFromDirectBytesIntoByteBuffer() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytesToTest = Bytes.fromDirect("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocateDirect(128);
        BufferUtil.limit(copyToDestination, (int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String(), "Bytes.wrapForRead");
    }

    @Test
    public void testCopyFromHeapBytesIntoByteBuffer() {
        Bytes<?> bytesToTest = Bytes.from("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocate(128);
        BufferUtil.limit(copyToDestination, (int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String(), "Bytes.wrapForRead");
    }
}
