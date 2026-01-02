/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

class AbstractInternerTest {

    private static final class TestInterner extends AbstractInterner<String> {
        TestInterner(int capacity) {
            super(capacity);
        }

        @Override
        protected String getValue(BytesStore<?, ?> bs, int length) {
            return bs.toString();
        }

        int pairKey(byte[] data) {
            BytesStore<?, ?> store = BytesStore.wrap(data);
            int hash = store.fastHash(0, data.length);
            int h = hash & mask;
            int h2 = (hash >> shift) & mask;
            return (h << 16) | h2;
        }
    }

    private TestInterner interner;

    @BeforeEach
    void setUp() {
        interner = new TestInterner(256);
    }

    @Test
    @DisplayName("intern cache returns the same Bytes reference instance")
    void internBytesObjectWithExistingInstance() {
        Bytes<?> bytes = Bytes.from("testString");
        String firstInterned = interner.intern(bytes);
        String secondInterned = interner.intern(bytes);
        Assertions.assertSame(firstInterned,
                secondInterned,
                "Interning the same Bytes should return the cached instance");
    }

    @Test
    @DisplayName("intern cache returns distinct Bytes instances for different input")
    void internBytesObjectWithNewInstance() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        String firstInterned = interner.intern(firstBytes);
        String secondInterned = interner.intern(secondBytes);
        Assertions.assertNotSame(firstInterned,
                secondInterned,
                "Interning different Bytes should yield distinct instances");
    }

    @Test
    @DisplayName("intern cache distinguishes Bytes inputs with different lengths")
    void internDifferentLengthBytes() {
        Bytes<?> shortBytes = Bytes.from("short");
        Bytes<?> longBytes = Bytes.from("aVeryLongStringIndeed");
        String shortInterned = interner.intern(shortBytes);
        String longInterned = interner.intern(longBytes);
        Assertions.assertNotSame(shortInterned,
                longInterned,
                "Different length inputs should not share the same interned instance");
    }

    @Test
    @DisplayName("value count grows after multiple intern operations")
    void valueCountWithMultipleInterns() {
        Bytes<?> firstBytes = Bytes.from("firstString");
        Bytes<?> secondBytes = Bytes.from("secondString");
        interner.intern(firstBytes);
        interner.intern(secondBytes);
        int count = interner.valueCount();
        Assertions.assertTrue(count >= 2,
                "Value count should be at least 2 after two interns, but was " + count);
    }

    @Test
    @DisplayName("toggle switches the active intern table in use")
    void toggleCheck() {
        Bytes<?> firstBytes = Bytes.from("toggleFirst");
        Bytes<?> secondBytes = Bytes.from("toggleSecond");
        interner.intern(firstBytes);
        interner.toggle();
        interner.intern(secondBytes);
        int count = interner.valueCount();
        Assertions.assertTrue(count >= 2,
                "Value count should include entries from both tables after toggle, but was " + count);
    }

    @Test
    @DisplayName("inputs longer than the table size are not cached")
    void internLongerThanTableSkipsCaching() {
        TestInterner longInterner = new TestInterner(8);
        byte[] payload = new byte[200];
        Bytes<byte[]> bytes = Bytes.wrapForRead(payload);
        try {
            int before = longInterner.valueCount();
            String value = longInterner.intern(bytes);
            int after = longInterner.valueCount();
            Assertions.assertEquals(0,
                    before,
                    "New interner should start with no cached values");
            Assertions.assertEquals(0,
                    after,
                    "Large inputs should not be cached in the interner table");
            Assertions.assertEquals(bytes.toString(),
                    value,
                    "Interned value should still reflect the underlying bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("intern accepts BytesStore with explicit length")
    void internBytesStoreWithLength() {
        Bytes<?> bytes = Bytes.from("storePayload");
        try {
            String value = interner.intern(bytes.bytesStore(), (int) bytes.readRemaining());
            Assertions.assertEquals(bytes.toString(),
                    value,
                    "Interning a BytesStore should return the decoded value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("intern resolves secondary slot and toggles when both slots are occupied")
    void internResolvesSecondarySlotAndToggle() {
        TestInterner local = new TestInterner(8);
        List<byte[]> candidates = findTripleCollision(local);
        Assertions.assertNotNull(candidates,
                "Expected to find a triple collision for hash slots");

        Bytes<byte[]> first = Bytes.wrapForRead(candidates.get(0));
        Bytes<byte[]> second = Bytes.wrapForRead(candidates.get(1));
        Bytes<byte[]> third = Bytes.wrapForRead(candidates.get(2));
        try {
            String firstValue = local.intern(first);
            String secondValue = local.intern(second);
            String secondAgain = local.intern(second);
            Assertions.assertSame(secondValue,
                    secondAgain,
                    "Second entry should be served from the secondary slot cache");
            Assertions.assertNotSame(firstValue,
                    secondValue,
                    "Distinct entries should be cached separately");

            String thirdValue = local.intern(third);
            Assertions.assertNotNull(thirdValue,
                    "Third entry should be interned after toggling");
        } finally {
            first.releaseLast();
            second.releaseLast();
            third.releaseLast();
        }
    }

    private static List<byte[]> findTripleCollision(TestInterner interner) {
        Map<Integer, List<byte[]>> collisions = new HashMap<>();
        for (int i = 0; i < 100000; i++) {
            byte[] bytes = ("key-" + i).getBytes(ISO_8859_1);
            int key = interner.pairKey(bytes);
            List<byte[]> bucket = collisions.computeIfAbsent(key, ignored -> new ArrayList<>());
            bucket.add(bytes);
            if (bucket.size() == 3) {
                return bucket;
            }
        }
        return null;
    }
}
