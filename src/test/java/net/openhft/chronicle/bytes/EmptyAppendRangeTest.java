/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmptyAppendRangeTest extends BytesTestCommon {
    @Test
    public void emptyStringSlicesPreserveExistingBytes() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        try {
            bytes.append("prefix");
            bytes.append("abc", 0, 0);
            bytes.append("abc", 1, 1);
            bytes.append("abc", 3, 3);
            bytes.append("", 0, 0);
            assertEquals("prefix", bytes.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void emptyBuilderSlicePreservesExistingBytes() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        try {
            bytes.append("prefix");
            bytes.append(new StringBuilder("abc"), 1, 1);
            assertEquals("prefix", bytes.toString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void emptySliceStillValidatesBounds() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        try {
            assertThrows(IndexOutOfBoundsException.class, () -> bytes.append("abc", 4, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> bytes.append("abc", -1, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> bytes.append("abc", 2, 1));
            assertEquals(0, bytes.writePosition());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void nonEmptyUtf16SliceRetainsCharacterOffsets() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        try {
            bytes.append("x\u03b1\u03b2z", 1, 3);
            assertEquals("\u03b1\u03b2", bytes.parseUtf8(StopCharTesters.ALL));
        } finally {
            bytes.releaseLast();
        }
    }
}
