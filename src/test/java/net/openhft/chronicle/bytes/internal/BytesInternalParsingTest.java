/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BytesInternalParsingTest extends BytesTestCommon {

    @Test
    public void parseBooleanTokens() {
        Bytes<?> t = Bytes.from("true");
        Bytes<?> f = Bytes.from("false");
        Bytes<?> y = Bytes.from("yes");
        Bytes<?> n = Bytes.from("no");
        Bytes<?> one = Bytes.from("1");
        Bytes<?> zero = Bytes.from("0");
        Bytes<?> maybe = Bytes.from("maybe");
        try {
            assertEquals(Boolean.TRUE, BytesInternal.parseBoolean(t, StopCharTesters.NON_ALPHA_DIGIT), "parseBoolean should return TRUE for input 'true'");
            assertEquals(Boolean.FALSE, BytesInternal.parseBoolean(f, StopCharTesters.NON_ALPHA_DIGIT), "parseBoolean should return FALSE for input 'false'");
            assertEquals(Boolean.TRUE, BytesInternal.parseBoolean(y, StopCharTesters.NON_ALPHA_DIGIT), "parseBoolean should return TRUE for input 'yes'");
            assertEquals(Boolean.FALSE, BytesInternal.parseBoolean(n, StopCharTesters.NON_ALPHA_DIGIT), "parseBoolean should return FALSE for input 'no'");
            assertEquals(Boolean.TRUE, BytesInternal.parseBoolean(one, StopCharTesters.NON_ALPHA_DIGIT), "parseBoolean should return TRUE for input '1'");
            assertEquals(Boolean.FALSE, BytesInternal.parseBoolean(zero, StopCharTesters.NON_ALPHA_DIGIT), "parseBoolean should return FALSE for input '0'");
            assertNull(BytesInternal.parseBoolean(maybe, StopCharTesters.NON_ALPHA_DIGIT), "parseBoolean should return null for unrecognized input 'maybe'");
        } finally {
            t.releaseLast();
            f.releaseLast();
            y.releaseLast();
            n.releaseLast();
            one.releaseLast();
            zero.releaseLast();
            maybe.releaseLast();
        }
    }

    @Test
    public void parseUtf8IntoBuilder() {
        Bytes<?> alpha = Bytes.from("alpha");
        Bytes<?> beta = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(alpha, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString(), "parseUtf8 should parse 'alpha' into StringBuilder");
            sb.setLength(0);
            BytesInternal.parseUtf8(beta, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("beta", sb.toString(), "parseUtf8 should parse 'beta' into StringBuilder after reset");
        } finally {
            alpha.releaseLast();
            beta.releaseLast();
        }
    }
}
