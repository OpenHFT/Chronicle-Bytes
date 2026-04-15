/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class UnicodeToStringTest {

    @Test
    public void testUtfStringInAndOut() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        bytes.appendUtf8("óaóó");
        assertEquals("óaóó", bytes.toUtf8String());
    }

    @Test
    public void testUtfStringInAndOutOnHeap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        bytes.appendUtf8("óaóó");
        assertEquals("óaóó", bytes.toUtf8String());
    }
}
