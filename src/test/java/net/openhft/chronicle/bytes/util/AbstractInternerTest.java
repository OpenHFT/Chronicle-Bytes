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
import net.openhft.chronicle.bytes.util.AbstractInterner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractInternerTest {

    private static final class TestInterner extends AbstractInterner<String> {
        protected TestInterner(int capacity) {
            super(capacity);
        }

        @Override
        protected String getValue(BytesStore<?, ?> bs, int length) {
            return bs.toString();
        }
    }

    private TestInterner interner;

    @BeforeEach
    void setUp() {
        interner = new TestInterner(256);
    }

    @Test
    void internBytesObjectWithExistingInstance() {
        Bytes<?> bytes = Bytes.from("testString");
        String firstInterned = interner.intern(bytes);
        String secondInterned = interner.intern(bytes);
        Assertions.assertSame(firstInterned, secondInterned);
    }

    @Test
    void internBytesObjectWithNewInstance() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        String firstInterned = interner.intern(firstBytes);
        String secondInterned = interner.intern(secondBytes);
        Assertions.assertNotSame(firstInterned, secondInterned);
    }

    @Test
    void internDifferentLengthBytes() {
        Bytes<?> shortBytes = Bytes.from("short");
        Bytes<?> longBytes = Bytes.from("aVeryLongStringIndeed");
        String shortInterned = interner.intern(shortBytes);
        String longInterned = interner.intern(longBytes);
        Assertions.assertNotSame(shortInterned, longInterned);
    }

    @Test
    void valueCountWithMultipleInterns() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        interner.intern(firstBytes);
        interner.intern(secondBytes);
        int count = interner.valueCount();
        Assertions.assertTrue(count >= 2);
    }

    @Test
    void toggleCheck() {
        Bytes<?> firstBytes = Bytes.from("toggleFirst");
        Bytes<?> secondBytes = Bytes.from("toggleSecond");
        interner.intern(firstBytes);
        interner.toggle();
        interner.intern(secondBytes);
        int count = interner.valueCount();
        Assertions.assertTrue(count >= 2);
    }
}
