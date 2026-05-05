/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests unchecked bytes behaviour because bypassing bounds checks requires
 * validation to ensure the correct wrapper is returned for direct memory.
 */
@DisplayName("UncheckedBytesBehaviour - validates unchecked wrapper selection")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class UncheckedBytesBehaviourTest extends BytesTestCommon {

    @Test
    @DisplayName("unchecked direct bytes append while heap bytes return same instance when false")
    public void uncheckedOnDirectAndNoopWhenFalse() {
        Bytes<?> d = Bytes.allocateDirect(16);
        Bytes<?> u = d.unchecked(true);
        try {
            u.append("zz");
            assertEquals("zz",
                    u.toString(),
                    "Unchecked direct bytes should preserve appended text");
        } finally {
            u.releaseLast();
        }

        Bytes<?> h = Bytes.allocateElasticOnHeap(8);
        try {
            Bytes<?> same = h.unchecked(false);
            assertSame(h,
                    same,
                    "Unchecked false should return the original heap bytes");
        } finally {
            h.releaseLast();
        }
    }
}
