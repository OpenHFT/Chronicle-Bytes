//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

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

    @Test
    public void uncheckedModeAllowsWritePastLimit() {
        Bytes<?> checked = Bytes.allocateElasticOnHeap(16);
        Bytes<?> unchecked = checked.unchecked(true);
        try {
            unchecked.writeLimit(4);
            unchecked.writeLong(0x0102030405060708L);
            assertEquals("Unchecked write should advance writePosition", 8, unchecked.writePosition());
            assertEquals("Checked view remains at start", 0, checked.readPosition());
        } finally {
            unchecked.releaseLast();
        }
    }

}
