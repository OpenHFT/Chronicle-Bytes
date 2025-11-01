/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UncheckedNativeBytesTest extends BytesTestCommon {

    @Test
    public void uncheckedWrapEnsureCapacityAndAppend() {
        Bytes<?> b = Bytes.allocateDirect(8);
        Bytes<?> u = b.unchecked(true);
        try {
            u.append("abc");
            assertEquals("abc", u.toString());
        } finally {
            u.releaseLast();
        }
    }
}
