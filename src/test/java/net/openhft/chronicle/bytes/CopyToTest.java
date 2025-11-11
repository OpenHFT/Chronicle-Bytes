/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class CopyToTest {

    @Test
    public void testCopyFromDirectBytesIntoByteBuffer() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytesToTest = Bytes.fromDirect("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocateDirect(128);
        copyToDestination.limit((int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String());
    }

    @Test
    public void testCopyFromHeapBytesIntoByteBuffer() {
        Bytes<?> bytesToTest = Bytes.from("THIS IS A TEST STRING");
        ByteBuffer copyToDestination = ByteBuffer.allocate(128);
        copyToDestination.limit((int) bytesToTest.readLimit());
        bytesToTest.copyTo(copyToDestination);
        assertEquals("THIS IS A TEST STRING", Bytes.wrapForRead(copyToDestination).toUtf8String());
    }
}
