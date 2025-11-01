/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class BinaryIntArrayReferenceTest extends BytesTestCommon {
    @Test
    public void getSetValues() {
        final int length = 128 * 4 + 2 * 8;
        final Bytes<?> bytes = Bytes.allocateDirect(length);
        try {
            BinaryIntArrayReference.write(bytes, 128);

            try (BinaryIntArrayReference array = new BinaryIntArrayReference()) {
                array.bytesStore(bytes, 0, length);

                assertEquals(128, array.getCapacity());
                for (int i = 0; i < 128; i++)
                    array.setValueAt(i, i + 1);

                for (int i = 0; i < 128; i++)
                    assertEquals(i + 1, array.getValueAt(i));
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void marshallable() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        assumeFalse(NativeBytes.areNewGuarded());
        final Bytes<?> bytes = Bytes.allocateElasticDirect(256);
        try {
            final IntArrays la = new IntArrays(4, 8);
            la.writeMarshallable(bytes);

            final String expected =
                    "00000000 04 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000020 08 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000030 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000040 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual);

            //System.out.println(bytes.toHexString());

            final IntArrays la2 = new IntArrays(0, 0);
            la2.readMarshallable(bytes);
            assertEquals(4, la2.first.getCapacity());
            assertEquals(8, la2.second.getCapacity());
            la.closeAll();
            la2.closeAll();
        } finally {
            bytes.releaseLast();
        }
    }

    private static final class IntArrays implements BytesMarshallable {
        BinaryIntArrayReference first = new BinaryIntArrayReference();
        BinaryIntArrayReference second = new BinaryIntArrayReference();

        public IntArrays(int firstLength, int secondLength) {
            first.capacity(firstLength);
            second.capacity(secondLength);
        }

        public void closeAll() {
            first.close();
            second.close();
        }
    }
}
