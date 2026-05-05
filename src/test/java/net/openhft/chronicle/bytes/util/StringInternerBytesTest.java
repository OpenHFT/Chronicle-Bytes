/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests string interner bytes because correct value counting is essential
 * to verify that repeated interns do not inflate the cache.
 */
@DisplayName("StringInternerBytes - validates string intern value counting")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class StringInternerBytesTest extends BytesTestCommon {

    @Test
    @DisplayName("interner tracks expected unique value count")
    public void testIntern() {
        @NotNull StringInternerBytes si = new StringInternerBytes(128);
        for (int i = 0; i < 100; i++) {
            Bytes<?> b = Bytes.from("key" + i);
            si.intern(b, (int) b.readRemaining());
            b.releaseLast();
        }
        assertEquals(89,
                si.valueCount(),
                "Interner should retain the expected number of unique values");
    }
}
