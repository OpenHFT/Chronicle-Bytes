/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextIntArrayReferenceTest extends BytesTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void getSetValues() {
        int length = 5 * 12 + 70;
        Bytes bytes = Bytes.allocateDirect(length);
        TextIntArrayReference.write(bytes, 5);

        try (@NotNull TextIntArrayReference array = new TextIntArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(5, array.getCapacity());
            for (int i = 0; i < 5; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 5; i++)
                assertEquals(i + 1, array.getValueAt(i));

            @NotNull final String expected = "{ locked: false, capacity: 5         , used: 0000000000, values: [ 0000000001, 0000000002, 0000000003, 0000000004, 0000000005 ] }\n";
            System.out.println(expected.length());
            assertEquals(expected,
                    bytes.toString());
            bytes.releaseLast();
        }
    }
}