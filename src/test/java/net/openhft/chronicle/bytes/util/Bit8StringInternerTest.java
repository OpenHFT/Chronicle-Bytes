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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
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
