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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
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
