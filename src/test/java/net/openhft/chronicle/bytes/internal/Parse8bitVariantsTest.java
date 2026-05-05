/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for parse8bit variants, because 8-bit string parsing must correctly
 * populate both StringBuilder and Bytes output targets for downstream processing.
 */
@DisplayName("parse8bit variants populate StringBuilder and Bytes correctly")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class Parse8bitVariantsTest extends BytesTestCommon {

    @Test
    @DisplayName("parse8bit fills StringBuilder with 'alpha' and Bytes with 'beta' from input")
    public void parse8bitIntoStringBuilderAndBytes() {
        Bytes<?> alpha = Bytes.from("alpha");
        Bytes<?> beta = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parse8bit(alpha, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString(),
                    "StringBuilder output matches expected alpha value");
            Bytes<?> out = Bytes.allocateElasticOnHeap(8);
            try {
                BytesInternal.parse8bit(beta, out, StopCharTesters.NON_ALPHA_DIGIT);
                assertEquals("beta", out.toString(),
                        "Bytes output matches expected beta value");
            } finally {
                out.releaseLast();
            }
        } finally {
            alpha.releaseLast();
            beta.releaseLast();
        }
    }
}
