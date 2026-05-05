/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests compact behaviour of Bytes across read, skip, append, and buffer state operations
 * because correct position reset is required to avoid buffer exhaustion during reuse.
 */
@SuppressWarnings({"checkstyle:MMOverusedWord", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Bytes - compact behaviour across native and heap allocators")
class BytesCompactTest {

    /**
     * Provides test data for parameterized tests.
     *
     * @return a collection of test scenarios with name and Bytes instances.
     */
    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("native", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(128)),
                Arguments.of("heap", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticOnHeap(128)),
                Arguments.of("unchecked native", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticDirect(128).unchecked(true)),
                Arguments.of("unchecked heap", (Supplier<Bytes<?>>) () -> Bytes.allocateElasticOnHeap(128).unchecked(true))
        );
    }

    /**
     * Test compact behavior of Bytes after various write and read operations.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("compact preserves unread bytes and positions")
    public void compact(String name, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            // Initialize buffer with a sample string
            bytes.clear().append("Hello World");

            // Parsing string until space character
            assertEquals("Hello", bytes.parse8bit(StopCharTesters.SPACE_STOP),
                    "parse8bit returns expected token for " + name);
            // Check the rest of the string
            assertEquals("World", bytes.toString(),
                    "remaining bytes form expected suffix for " + name);
            // Assert the read position
            assertEquals(6, bytes.readPosition(),
                    "read position advances after parsing for " + name);
            // Assert the number of unread bytes
            assertEquals(5, bytes.readRemaining(),
                    "read remaining matches expected count for " + name);

            // Compact the buffer
            bytes.compact();

            // Assert the buffer state after compacting
            assertEquals("World", bytes.toString(),
                    "compact preserves unread suffix for " + name);
            assertEquals(0, bytes.readPosition(),
                    "compact resets read position for " + name);
            assertEquals(5, bytes.readRemaining(),
                    "compact keeps unread length for " + name);

            // Append more to the buffer
            bytes.append("!?");

            // Read a character and assert the buffer state
            assertEquals('W', bytes.readChar(),
                    "readChar returns first unread character for " + name);
            assertEquals("orld!?", bytes.toString(),
                    "remaining bytes include appended suffix for " + name);
            assertEquals(1, bytes.readPosition(),
                    "read position advances after readChar for " + name);
            assertEquals(6, bytes.readRemaining(),
                    "read remaining includes appended bytes for " + name);

            // Compact again and assert the buffer state
            bytes.compact();
            assertEquals("orld!?", bytes.toString(),
                    "second compact preserves unread bytes for " + name);
            assertEquals(0, bytes.readPosition(),
                    "second compact resets read position for " + name);
            assertEquals(6, bytes.readRemaining(),
                    "second compact keeps unread length for " + name);
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * Test compact behaviour of Bytes when skipping bytes and preserving positions.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("compact after skip maintains expected read positions")
    public void skipCompact(String name, Supplier<Bytes<?>> supplier) {
        Bytes<?> bytes = supplier.get();
        try {
            // Clear and move the write position 64 bytes ahead
            bytes.clear().writeSkip(64);

            // Expected read positions after each read and compact operation
            int[] pos = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 0, 1, 2, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            // Loop to read a byte, compact and assert the read position
            for (int i = 0; i <= 64; i++) {
                bytes.compact();
                assertEquals(pos[i], bytes.readPosition(),
                        "compact read position matches expected at index " + i + " for " + name);
                bytes.readUnsignedByte();
            }
        } finally {
            bytes.releaseLast();
        }
    }
}
