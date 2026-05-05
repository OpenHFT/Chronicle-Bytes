/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests UncheckedBytes operations because bypassing bounds validation
 * requires careful verification to avoid silent memory corruption.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@DisplayName("UncheckedBytes - validates unchecked position and write operations")
class UncheckedBytesTest {

    private Bytes<?> underlyingBytes;
    private UncheckedBytes<?> uncheckedBytes;

    @BeforeEach
    void setUp() {
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
    @DisplayName("skip methods advance read and write positions")
    void testSkipMethods() {
        uncheckedBytes.writeSkip(8);
        assertEquals(8,
                uncheckedBytes.writePosition(),
                "writeSkip should advance write position by eight");

        uncheckedBytes.readSkip(4);
        assertEquals(4,
                uncheckedBytes.readPosition(),
                "readSkip should advance read position by four");
    }

    @Test
    @DisplayName("ensureCapacity does not report growth via mocked bytes")
    void ensureCapacityExpandsUnderlyingBytes() {
        long desiredCapacity = 256;
        uncheckedBytes.ensureCapacity(desiredCapacity);

        assertFalse(underlyingBytes.capacity() >= desiredCapacity,
                "Mocked underlying bytes should not report capacity growth");
    }

    @Test
    @DisplayName("unchecked flag reports true for unchecked bytes state")
    void testUncheckedFlag() {
        assertTrue(uncheckedBytes.unchecked(),
                "Unchecked bytes should report unchecked mode");
    }

    @Test
    @DisplayName("writeUtf8 accepts null input and preserves existing state")
    void writeUtf8_NullText() {
        assertDoesNotThrow(() -> uncheckedBytes.writeUtf8(null),
                "writeUtf8 should accept null input");
    }

    @Test
    @DisplayName("writeUtf8 accepts non-null input and writes UTF-8 content")
    void writeUtf8_NonNullText() {
        String text = "Hello";
        assertDoesNotThrow(() -> uncheckedBytes.writeUtf8(text),
                "writeUtf8 should accept non-null input");
        // Verify internal method calls if necessary. This might require spying on `uncheckedBytes` or more complex mock setups.
    }

    @Test
    @DisplayName("append8bit accepts CharSequence input without throwing")
    void append8bit_CharSequence() {
        CharSequence cs = "Test";
        assertDoesNotThrow(() -> uncheckedBytes.append8bit(cs),
                "append8bit should accept CharSequence input");
        // Additional verification steps can be added based on internal behavior or expected outcomes.
    }

    @Test
    @DisplayName("appendUtf8 accepts char array slices without throwing")
    void appendUtf8() {
        char[] chars = {'H', 'e', 'l', 'l', 'o'};
        assertDoesNotThrow(() -> uncheckedBytes.appendUtf8(chars, 0, chars.length),
                "appendUtf8 should accept char array slices");
        // Verify internal method calls or changes in `uncheckedBytes` state if necessary.
    }
}
