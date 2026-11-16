/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TextArrayUsedCountTest {
    @Test
    public void increasingIntUsedCountPreservesFirstElement() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextIntArrayReference array = new TextIntArrayReference()) {
            TextIntArrayReference.write(bytes, 3);
            array.bytesStore(bytes, 0, bytes.writePosition());
            array.setValueAt(0, 42);
            array.setMaxUsed(2);
            assertEquals(2, array.getUsed());
            assertEquals(42, array.getValueAt(0));
            array.setMaxUsed(1);
            assertEquals(2, array.getUsed());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void increasingLongUsedCountPreservesFirstElement() {
        Bytes<?> bytes = Bytes.allocateDirect(256);
        try (TextLongArrayReference array = new TextLongArrayReference()) {
            TextLongArrayReference.write(bytes, 3);
            array.bytesStore(bytes, 0, bytes.writePosition());
            array.setValueAt(0, 1L << 40);
            array.setMaxUsed(2);
            assertEquals(2, array.getUsed());
            assertEquals(1L << 40, array.getValueAt(0));
            array.setMaxUsed(1);
            assertEquals(2, array.getUsed());
        } finally {
            bytes.releaseLast();
        }
    }
}
