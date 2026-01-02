/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BytesInternal contentEqual comparisons across store implementations")
public class BytesInternalContentEqualTest extends BytesTestCommon {

    @Test
    @DisplayName("heap and direct stores compare equal for same content")
    public void heapVsDirectEqualContent() {
        Bytes<?> heap = Bytes.from("abcdef");
        Bytes<?> direct = Bytes.allocateDirect(6);
        try {
            direct.append("abcdef");
            // ensure comparisons start from position 0
            heap.readPosition(0);
            direct.readPosition(0);

            assertTrue(BytesInternal.contentEqual(heap.bytesStore(), direct.bytesStore()),
                    "Expected equal content across heap and direct stores");
        } finally {
            heap.releaseLast();
            direct.releaseLast();
        }
    }

    @Test
    @DisplayName("different lengths are not equal across stores")
    public void differentLengthsAreNotEqual() {
        Bytes<?> left = Bytes.from("abc");
        Bytes<?> right = Bytes.from("abcd");
        try {
            assertFalse(BytesInternal.contentEqual(left.bytesStore(), right.bytesStore()),
                    "Different store lengths are not equal");
        } finally {
            left.releaseLast();
            right.releaseLast();
        }
    }

    @Test
    @DisplayName("single byte mismatch is detected across stores")
    public void singleByteMismatchDetected() {
        Bytes<?> left = Bytes.from("abcde");
        Bytes<?> right = Bytes.from("abXde");
        try {
            assertFalse(BytesInternal.contentEqual(left.bytesStore(), right.bytesStore()),
                    "Single byte mismatch is detected across heap stores");
        } finally {
            left.releaseLast();
            right.releaseLast();
        }
    }
}
