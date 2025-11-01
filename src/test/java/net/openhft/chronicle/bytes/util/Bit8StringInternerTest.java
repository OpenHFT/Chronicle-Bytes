/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Bit8StringInternerTest {

    @Test
    public void testGetValue() {
        Bytes<byte[]> bytesStore = Bytes.from("Hello World");
        int length = (int) bytesStore.readRemaining();

        Bit8StringInterner interner = new Bit8StringInterner(16);

        String internedString = interner.getValue(bytesStore, length);

        assertEquals("Hello World", internedString);
    }
}
