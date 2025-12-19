/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.util.AbstractInterner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
class AbstractInternerTest {

    private static final class SampleInterner extends AbstractInterner<String> {
        SampleInterner(int capacity) {
            super(capacity);
        }

        @Override
        protected String getValue(BytesStore<?, ?> bs, int length) {
            return bs.toString();
        }
    }

    private SampleInterner interner;

    @BeforeEach
    void setUp() {
        interner = new SampleInterner(256);
    }

    @Test
    void internBytesObjectWithExistingInstance() {
        Bytes<?> bytes = Bytes.from("testString");
        String firstInterned = interner.intern(bytes);
        String secondInterned = interner.intern(bytes);
        assertSame(firstInterned, secondInterned, "interning same bytes twice should return same instance");
    }

    @Test
    void internBytesObjectWithNewInstance() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        String firstInterned = interner.intern(firstBytes);
        String secondInterned = interner.intern(secondBytes);
        assertNotSame(firstInterned, secondInterned, "interning different bytes should return different instances");
    }

    @Test
    void internDifferentLengthBytes() {
        Bytes<?> shortBytes = Bytes.from("short");
        Bytes<?> longBytes = Bytes.from("aVeryLongStringIndeed");
        String shortInterned = interner.intern(shortBytes);
        String longInterned = interner.intern(longBytes);
        assertNotSame(shortInterned, longInterned, "interning bytes with different lengths should return different instances");
    }

    @Test
    void valueCountWithMultipleInterns() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        interner.intern(firstBytes);
        interner.intern(secondBytes);
        int count = interner.valueCount();
        assertTrue(count >= 2, "interning 2 different strings should result in valueCount >= 2");
    }

    @Test
    void toggleCheck() {
        Bytes<?> firstBytes = Bytes.from("toggleFirst");
        Bytes<?> secondBytes = Bytes.from("toggleSecond");
        interner.intern(firstBytes);
        interner.toggle();
        interner.intern(secondBytes);
        int count = interner.valueCount();
        assertTrue(count >= 2, "toggle() should not prevent valueCount from tracking interned strings");
    }
}
