/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests 8-bit string interner because correct ISO-8859-1 decoding is
 * essential to cache single-byte character strings efficiently.
 */
@DisplayName("Bit8StringInterner - validates 8-bit string caching")
public class Bit8StringInternerTest {

    @Test
    @DisplayName("interner returns stored string for 8-bit bytes")
    public void testGetValue() {
        Bytes<byte[]> bytesStore = Bytes.from("Hello World");
        int length = (int) bytesStore.readRemaining();

        Bit8StringInterner interner = new Bit8StringInterner(16);

        String internedString = interner.getValue(bytesStore, length);

        assertEquals("Hello World",
                internedString,
                "Interner should return the original string for 8-bit bytes");
    }
}
