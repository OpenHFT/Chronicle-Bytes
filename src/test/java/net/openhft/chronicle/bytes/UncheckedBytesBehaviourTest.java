/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class UncheckedBytesBehaviourTest extends BytesTestCommon {

    @Test
    public void uncheckedOnDirectAndNoopWhenFalse() {
        Bytes<?> d = Bytes.allocateDirect(16);
        Bytes<?> u = d.unchecked(true);
        try {
            u.append("zz");
            assertEquals("zz", u.toString(), "UncheckedBytes should contain appended string content");
        } finally {
            u.releaseLast();
        }

        Bytes<?> h = Bytes.allocateElasticOnHeap(8);
        try {
            Bytes<?> same = h.unchecked(false);
            assertSame(h, same, "unchecked(false) on heap bytes should return same instance (no-op)");
        } finally {
            h.releaseLast();
        }
    }

    @Test
    public void uncheckedModeAllowsWritePastLimit() {
        Bytes<?> checked = Bytes.allocateElasticOnHeap(16);
        Bytes<?> unchecked = checked.unchecked(true);
        try {
            unchecked.writeLimit(4);
            unchecked.writeLong(0x0102030405060708L);
            assertEquals(8, unchecked.writePosition(), "Unchecked write should advance writePosition");
            assertEquals(0, checked.readPosition(), "Checked view remains at start");
        } finally {
            unchecked.releaseLast();
        }
    }
}
