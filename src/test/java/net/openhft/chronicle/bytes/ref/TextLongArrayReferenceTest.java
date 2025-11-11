/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextLongArrayReferenceTest extends BytesTestCommon {
    @Test
    public void getSetValues() {
        int length = 5 * 22 + 90;
        Bytes<?> bytes = Bytes.allocateElastic(length);
        TextLongArrayReference.write(bytes, 5);

        try (@NotNull TextLongArrayReference array = new TextLongArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(5, array.getCapacity());
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1, array.getValueAt(i));

            @NotNull final String expected = "{ locked: false, capacity: 5                   , used: 00000000000000000000, " +
                    "values: [ 00000000000000000001, 00000000000000000002, 00000000000000000003, 00000000000000000004, 00000000000000000005 ] }\n";
//            System.out.println(expected.length());
            assertEquals(expected,
                    bytes.toString());
            bytes.releaseLast();
        }
    }
}
