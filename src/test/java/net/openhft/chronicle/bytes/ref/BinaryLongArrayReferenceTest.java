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

import net.openhft.chronicle.bytes.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class BinaryLongArrayReferenceTest extends BytesTestCommon {
    @Test
    public void getSetValues() {
        final int length = 128 * 8 + 2 * 8;
        final Bytes<?> bytes = Bytes.allocateDirect(length);
        try {
            BinaryLongArrayReference.write(bytes, 128);

            try (BinaryLongArrayReference array = new BinaryLongArrayReference()) {
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
        final Bytes<?> bytes = new HexDumpBytes();
        try {
            final LongArrays la = new LongArrays(4, 8);
            la.writeMarshallable(bytes);

            final String expected =
                    "                                                # first\n" +
                            "                                                # BinaryLongArrayReference\n" +
                            "   04 00 00 00 00 00 00 00                         # capacity\n" +
                            "   00 00 00 00 00 00 00 00                         # used\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # second\n" +
                            "                                                # BinaryLongArrayReference\n" +
                            "   08 00 00 00 00 00 00 00                         # capacity\n" +
                            "   00 00 00 00 00 00 00 00                         # used\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 # values\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                            "   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual);

            //System.out.println(bytes.toHexString());

            final LongArrays la2 = new LongArrays(0, 0);
            la2.readMarshallable(bytes);
            assertEquals(4, la2.first.getCapacity());
            assertEquals(8, la2.second.getCapacity());
            la.closeAll();
            la2.closeAll();
        } finally {
            bytes.releaseLast();
        }
    }

    private static final class LongArrays implements BytesMarshallable {
        BinaryLongArrayReference first = new BinaryLongArrayReference();
        BinaryLongArrayReference second = new BinaryLongArrayReference();

        public LongArrays(int firstLength, int secondLength) {
            first.capacity(firstLength);
            second.capacity(secondLength);
        }

        public void closeAll() {
            first.close();
            second.close();
        }
    }
}