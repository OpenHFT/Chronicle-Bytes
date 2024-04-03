/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the compact behavior of Bytes.
 */
@RunWith(Parameterized.class)
public class BytesCompactTest {

    final String name;
    final Bytes<?> bytes;

    /**
     * Constructor for parameterized test with name and bytes.
     *
     * @param name  the name of the test scenario.
     * @param bytes the Bytes instance under test.
     */
    public BytesCompactTest(String name, Bytes<?> bytes) {
        this.name = name;
        this.bytes = bytes;
    }

    /**
     * Provides test data for parameterized tests.
     *
     * @return a collection of test scenarios with name and Bytes instances.
     */
    @Parameterized.Parameters(name = "{0}")
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
    @Test
    public void compact() {
        // Initialize buffer with a sample string
        bytes.clear().append("Hello World");

        // Parsing string until space character
        assertEquals("Hello", bytes.parse8bit(StopCharTesters.SPACE_STOP));
        // Check the rest of the string
        assertEquals("World", bytes.toString());
        // Assert the read position
        assertEquals(6, bytes.readPosition());
        // Assert the number of unread bytes
        assertEquals(5, bytes.readRemaining());

        // Compact the buffer
        bytes.compact();

        // Assert the buffer state after compacting
        assertEquals("World", bytes.toString());
        assertEquals(0, bytes.readPosition());
        assertEquals(5, bytes.readRemaining());

        // Append more to the buffer
        bytes.append("!?");

        // Read a character and assert the buffer state
        assertEquals('W', bytes.readChar());
        assertEquals("orld!?", bytes.toString());
        assertEquals(1, bytes.readPosition());
        assertEquals(6, bytes.readRemaining());

        // Compact again and assert the buffer state
        bytes.compact();
        assertEquals("orld!?", bytes.toString());
        assertEquals(0, bytes.readPosition());
        assertEquals(6, bytes.readRemaining());
    }

    /**
     * Test compact behavior of Bytes when skipping bytes.
     */
    @Test
    public void skipCompact() {
        // Clear and move the write position 64 bytes ahead
        bytes.clear().writeSkip(64);

        // Expected read positions after each read and compact operation
        int[] pos = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 0, 1, 2, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        // Loop to read a byte, compact and assert the read position
        for (int i = 0; i <= 64; i++) {
            bytes.compact();
            assertEquals(pos[i], bytes.readPosition());
            bytes.readUnsignedByte();
        }
    }
}
