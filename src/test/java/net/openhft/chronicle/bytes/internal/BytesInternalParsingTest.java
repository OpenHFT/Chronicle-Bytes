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
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("BytesInternal parsing for booleans and UTF8 segments")
public class BytesInternalParsingTest extends BytesTestCommon {

    @Test
    @DisplayName("parse boolean tokens with stop char testers")
    public void parseBooleanTokens() {
        Bytes<?> t = Bytes.from("true");
        Bytes<?> f = Bytes.from("false");
        Bytes<?> y = Bytes.from("yes");
        Bytes<?> n = Bytes.from("no");
        Bytes<?> one = Bytes.from("1");
        Bytes<?> zero = Bytes.from("0");
        Bytes<?> maybe = Bytes.from("maybe");
        try {
            assertEquals(Boolean.TRUE,
                    BytesInternal.parseBoolean(t, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean true token");
            assertEquals(Boolean.FALSE,
                    BytesInternal.parseBoolean(f, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean false token");
            assertEquals(Boolean.TRUE,
                    BytesInternal.parseBoolean(y, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean yes token");
            assertEquals(Boolean.FALSE,
                    BytesInternal.parseBoolean(n, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean no token");
            assertEquals(Boolean.TRUE,
                    BytesInternal.parseBoolean(one, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean one token");
            assertEquals(Boolean.FALSE,
                    BytesInternal.parseBoolean(zero, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean zero token");
            assertNull(BytesInternal.parseBoolean(maybe, StopCharTesters.NON_ALPHA_DIGIT),
                    "Unknown boolean token yields null");
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
    @DisplayName("parse UTF8 into builder with stop tester")
    public void parseUtf8IntoBuilder() {
        Bytes<?> alpha = Bytes.from("alpha");
        Bytes<?> beta = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(alpha, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString(),
                    "UTF8 parse reads alpha");
            sb.setLength(0);
            BytesInternal.parseUtf8(beta, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("beta", sb.toString(),
                    "UTF8 parse reads beta");
        } finally {
            alpha.releaseLast();
            beta.releaseLast();
        }
    }
}
