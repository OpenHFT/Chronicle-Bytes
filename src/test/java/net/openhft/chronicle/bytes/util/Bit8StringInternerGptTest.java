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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for Bit8StringInterner.
 *
 * @author OpenHFT
 */
public class Bit8StringInternerGptTest {

    /**
     * Test the getValue method to convert bytes to string.
     */
    @Test
    public void testGetValue() {
        // Create an interner with capacity 256
        Bit8StringInterner interner = new Bit8StringInterner(256);

        // Create bytes of "hello" string
        Bytes<?> bytes = Bytes.from("hello");

        // Use interner to convert bytes to string
        String value = interner.getValue(bytes, 5);

        // Assert that the returned string is equal to "hello"
        Assert.assertEquals("hello", value);
    }

    /**
     * Test Bit8StringInterner with invalid capacity.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCapacity() {
        new Bit8StringInterner(-1);
    }
}
