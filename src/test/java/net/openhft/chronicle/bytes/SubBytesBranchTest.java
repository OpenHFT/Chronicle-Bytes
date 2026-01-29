/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests SubBytes branch coverage because correct sub-region views are
 * essential to avoid reading outside the intended slice when positions advance.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("SubBytes - validates sub-region view behaviour after position advance")
class SubBytesBranchTest extends BytesTestCommon {

    private Bytes<?> parentBytes;

    @BeforeEach
    void setUp() {
        parentBytes = Bytes.allocateElasticDirect(256);
        parentBytes.append("Hello World - This is test data for SubBytes testing!");
    }

    @AfterEach
    void tearDown() {
        if (parentBytes != null) {
            parentBytes.releaseLast();
            parentBytes = null;
        }
    }

    @Test
    @DisplayName("bytesForRead creates SubBytes after readPosition advance")
    void bytesForReadCreatesSubBytes() {
        // Advance readPosition to create a SubBytes
        parentBytes.readPosition(6);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            assertTrue(sub instanceof SubBytes, "bytesForRead returns SubBytes after readPosition advance");
            assertTrue(sub.toString().startsWith("World"),
                    "SubBytes content starts with World: " + sub.toString());
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes start returns correct offset value")
    void subBytesStartReturnsOffset() {
        parentBytes.readPosition(6);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            assertEquals(6, sub.start(), "SubBytes start offset equals 6");
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes capacity returns expected range values")
    void subBytesCapacityReturnsValue() {
        parentBytes.readPosition(6);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            // Capacity should be the size of the sub-region
            assertTrue(sub.capacity() > 0, "SubBytes capacity is positive");
            assertTrue(sub.capacity() >= sub.readRemaining(),
                    "SubBytes capacity is at least readRemaining");
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes realCapacity equals capacity because non-elastic views have fixed size")
    void subBytesRealCapacityEqualsCapacity() {
        parentBytes.readPosition(10);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            assertEquals(sub.capacity(), sub.realCapacity(),
                    "SubBytes realCapacity should equal capacity because the view is non-elastic and backed by a fixed region");
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("bytesForRead from start provides non-null view so callers can read")
    void bytesForReadFromStartNotSubBytes() {
        // When readPosition is at start, bytesForRead returns store's bytesForRead
        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            // At start position, SubBytes may or may not be created
            assertNotNull(sub, "bytesForRead should return a non-null view so callers can read from the current position");
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes reads content from advanced position")
    void subBytesCanReadContent() {
        parentBytes.readPosition(6);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            assertEquals('W', (char) sub.readByte(), "first character equals W");
            assertEquals('o', (char) sub.readByte(), "second character equals o");
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes toString returns remaining content text")
    void subBytesToStringReturnsContent() {
        parentBytes.readPosition(6);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            String content = sub.toString();
            assertTrue(content.startsWith("World"),
                    "toString content starts with World: " + content);
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes start reflects different read positions")
    void subBytesWithDifferentStarts() {
        // Test with position near start
        parentBytes.readPosition(1);
        Bytes<?> sub1 = parentBytes.bytesForRead();
        try {
            assertEquals(1, sub1.start(), "SubBytes start offset equals 1");
        } finally {
            sub1.releaseLast();
        }

        // Reset and test with position in middle
        parentBytes.readPosition(12);
        Bytes<?> sub2 = parentBytes.bytesForRead();
        try {
            assertEquals(12, sub2.start(), "SubBytes start offset equals 12");
        } finally {
            sub2.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes reports non-elastic view state")
    void subBytesIsNonElastic() {
        parentBytes.readPosition(5);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            assertFalse(sub.isElastic(), "SubBytes is non-elastic");
        } finally {
            sub.releaseLast();
        }
    }

    @Test
    @DisplayName("multiple SubBytes views can be created safely")
    void multipleSubBytesViews() {
        // First view from position 0
        parentBytes.readPosition(0);
        Bytes<?> sub1 = parentBytes.bytesForRead();

        // Second view from a later position
        parentBytes.readPosition(6);
        Bytes<?> sub2 = parentBytes.bytesForRead();

        try {
            assertTrue(sub1.toString().startsWith("Hello"),
                    "first SubBytes starts with Hello: " + sub1.toString());
            assertTrue(sub2.toString().startsWith("World"),
                    "second SubBytes starts with World: " + sub2.toString());
        } finally {
            sub1.releaseLast();
            sub2.releaseLast();
        }
    }

    @Test
    @DisplayName("SubBytes clear resets read position to start")
    void subBytesClearResetsPositions() {
        parentBytes.readPosition(10);

        Bytes<?> sub = parentBytes.bytesForRead();
        try {
            // Read some data to advance position
            sub.readByte();
            sub.readByte();

            // Clear should reset positions
            sub.clear();
            assertEquals(sub.start(), sub.readPosition(), "readPosition resets to start after clear");
        } finally {
            sub.releaseLast();
        }
    }
}
