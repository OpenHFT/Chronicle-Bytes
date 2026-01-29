/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests Unicode-to-string conversion because correct encoding is essential
 * to preserve multi-byte characters during round-trip serialisation.
 */
@DisplayName("UnicodeToString - validates UTF-8 string round-trip conversion")
public class UnicodeToStringTest {

    @Test
    @DisplayName("UTF-8 string round trips using direct byte buffer")
    public void testUtfStringInAndOut() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for this UTF-8 test");

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        bytes.appendUtf8("óaóó");
        assertEquals("óaóó",
                bytes.toUtf8String(),
                "UTF-8 string should round trip through direct bytes");
    }

    @Test
    @DisplayName("UTF-8 string round trips using heap byte buffer")
    public void testUtfStringInAndOutOnHeap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        bytes.appendUtf8("óaóó");
        assertEquals("óaóó",
                bytes.toUtf8String(),
                "UTF-8 string should round trip through heap bytes");
    }
}
