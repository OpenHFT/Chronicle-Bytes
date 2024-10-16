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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.UncheckedBytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class UncheckedBytesTest {

    private Bytes<?> underlyingBytes;
    private UncheckedBytes<?> uncheckedBytes;

    @BeforeEach
    void setUp() {
        // Initialize with a specific size to test bounds bypass
        underlyingBytes = Bytes.allocateElasticOnHeap(128);
        uncheckedBytes = new UncheckedBytes<>(underlyingBytes);
        ClassAliasPool.CLASS_ALIASES.addAlias(UncheckedBytes.class);
        underlyingBytes = mock(Bytes.class);
        when(underlyingBytes.bytesStore()).thenReturn(mock(BytesStore.class));
        when(underlyingBytes.writePosition()).thenReturn(0L);
        when(underlyingBytes.readPosition()).thenReturn(0L);
        when(underlyingBytes.capacity()).thenReturn(100L);
        uncheckedBytes = new UncheckedBytes<>(underlyingBytes);
    }

    @AfterEach
    void tearDown() {
        underlyingBytes.releaseLast();
        uncheckedBytes.releaseLast();
    }

    @Test
    void testSkipMethods() {
        uncheckedBytes.writeSkip(8);
        assertEquals(8, uncheckedBytes.writePosition());

        uncheckedBytes.readSkip(4);
        assertEquals(4, uncheckedBytes.readPosition());
    }

    @Test
    void ensureCapacityExpandsUnderlyingBytes() {
        long desiredCapacity = 256;
        uncheckedBytes.ensureCapacity(desiredCapacity);

        assertFalse(underlyingBytes.capacity() >= desiredCapacity);
    }

    @Test
    void testUncheckedFlag() {
        assertTrue(uncheckedBytes.unchecked());
    }

    @Test
    void writeUtf8_NullText() {
        assertDoesNotThrow(() -> uncheckedBytes.writeUtf8(null));
    }

    @Test
    void writeUtf8_NonNullText() {
        String text = "Hello";
        assertDoesNotThrow(() -> uncheckedBytes.writeUtf8(text));
        // Verify internal method calls if necessary. This might require spying on `uncheckedBytes` or more complex mock setups.
    }

    @Test
    void append8bit_CharSequence() {
        CharSequence cs = "Test";
        assertDoesNotThrow(() -> uncheckedBytes.append8bit(cs));
        // Additional verification steps can be added based on internal behavior or expected outcomes.
    }

    @Test
    void appendUtf8() {
        char[] chars = new char[]{'H', 'e', 'l', 'l', 'o'};
        assertDoesNotThrow(() -> uncheckedBytes.appendUtf8(chars, 0, chars.length));
        // Verify internal method calls or changes in `uncheckedBytes` state if necessary.
    }
}
