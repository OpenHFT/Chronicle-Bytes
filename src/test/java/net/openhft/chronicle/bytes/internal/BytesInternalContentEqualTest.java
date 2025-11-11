/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BytesInternalContentEqualTest extends BytesTestCommon {

    @Test
    public void heapVsDirectEqualContent() {
        Bytes<?> heap = Bytes.from("abcdef");
        Bytes<?> direct = Bytes.allocateDirect(6);
        try {
            direct.append("abcdef");
            // ensure comparisons start from position 0
            heap.readPosition(0);
            direct.readPosition(0);

            assertTrue("Expected equal content across heap and direct stores",
                    BytesInternal.contentEqual(heap.bytesStore(), direct.bytesStore()));
        } finally {
            heap.releaseLast();
            direct.releaseLast();
        }
    }

    @Test
    public void differentLengthsAreNotEqual() {
        Bytes<?> left = Bytes.from("abc");
        Bytes<?> right = Bytes.from("abcd");
        try {
            assertFalse(BytesInternal.contentEqual(left.bytesStore(), right.bytesStore()));
        } finally {
            left.releaseLast();
            right.releaseLast();
        }
    }

    @Test
    public void singleByteMismatchDetected() {
        Bytes<?> left = Bytes.from("abcde");
        Bytes<?> right = Bytes.from("abXde");
        try {
            assertFalse(BytesInternal.contentEqual(left.bytesStore(), right.bytesStore()));
        } finally {
            left.releaseLast();
            right.releaseLast();
        }
    }
}
