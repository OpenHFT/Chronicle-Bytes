/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the compact behavior of Bytes.
 */
class BytesCompactTest {

    private String name;
    private Bytes<?> bytes;

    /**
     * Constructor for parameterized test with name and bytes.
     *
     * @param name  the name of the test scenario.
     * @param bytes the Bytes instance under test.
     */
    public void initBytesCompactTest(String name, Bytes<?> bytes) {
        this.name = name;
        this.bytes = bytes;
    }

    /**
     * Provides test data for parameterized tests.
     *
     * @return a collection of test scenarios with name and Bytes instances.
     */
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"native", Bytes.allocateElasticDirect(128)},
                {"heap", Bytes.allocateElasticOnHeap(128)},
                {"unchecked native", Bytes.allocateElasticDirect(128).unchecked(true)},
                {"unchecked heap", Bytes.allocateElasticOnHeap(128).unchecked(true)}
        });
    }

    /**
     * Test compact behavior of Bytes after various write and read operations.
     */
    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void compact(String name, Bytes<?> bytes) {
        initBytesCompactTest(name, bytes);
        assertNotNull(name, "test name parameter should be non-null");
        // Initialize buffer with a sample string
        bytes.clear().append("Hello World");

        // Parsing string until space character
        assertEquals("Hello", bytes.parse8bit(StopCharTesters.SPACE_STOP), "parse8bit value");
        // Check the rest of the string
        assertEquals("World", bytes.toString(), "toString should show remaining unread bytes after parsing 'Hello'");
        // Assert the read position
        assertEquals(6, bytes.readPosition(), "read position should be at 6 after parsing 'Hello' and space");
        // Assert the number of unread bytes
        assertEquals(5, bytes.readRemaining(), "readRemaining value");

        // Compact the buffer
        bytes.compact();

        // Assert the buffer state after compacting
        assertEquals("World", bytes.toString(), "toString should show 'World' after compacting unread bytes to start");
        assertEquals(0, bytes.readPosition(), "read position should reset to 0 after compact operation");
        assertEquals(5, bytes.readRemaining(), "readRemaining value");

        // Append more to the buffer
        bytes.append("!?");

        // Read a character and assert the buffer state
        assertEquals('W', bytes.readChar(), "readChar value");
        assertEquals("orld!?", bytes.toString(), "toString should show remaining unread bytes after reading 'W' character");
        assertEquals(1, bytes.readPosition(), "read position should be at 1 after reading single character");
        assertEquals(6, bytes.readRemaining(), "readRemaining value");

        // Compact again and assert the buffer state
        bytes.compact();
        assertEquals("orld!?", bytes.toString(), "toString should show 'orld!?' after compacting remaining bytes to start");
        assertEquals(0, bytes.readPosition(), "read position should reset to 0 after second compact operation");
        assertEquals(6, bytes.readRemaining(), "readRemaining value");
    }

    /**
     * Test compact behavior of Bytes when skipping bytes.
     */
    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void skipCompact(String name, Bytes<?> bytes) {
        initBytesCompactTest(name, bytes);
        // Clear and move the write position 64 bytes ahead
        bytes.clear().writeSkip(64);

        // Expected read positions after each read and compact operation
        int[] pos = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 0, 1, 2, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        // Loop to read a byte, compact and assert the read position
        for (int i = 0; i <= 64; i++) {
            bytes.compact();
            assertEquals(pos[i], bytes.readPosition(), "read position should be " + pos[i] + " after compact at iteration " + i);
            bytes.readUnsignedByte();
        }
    }
}
