/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.*;

public class UncheckedBytesBehaviourTest extends BytesTestCommon {

    @Test
    public void uncheckedOnDirectAndNoopWhenFalse() {
        Bytes<?> d = Bytes.allocateDirect(16);
        Bytes<?> u = d.unchecked(true);
        try {
            u.append("zz");
            assertEquals("zz", u.toString());
        } finally {
            u.releaseLast();
        }

        Bytes<?> h = Bytes.allocateElasticOnHeap(8);
        try {
            Bytes<?> same = h.unchecked(false);
            assertSame(h, same);
        } finally {
            h.releaseLast();
        }
    }
}
