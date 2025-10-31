/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.Test;

import static org.junit.Assert.*;

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
            assertEquals(Boolean.TRUE, BytesInternal.parseBoolean(t, StopCharTesters.NON_ALPHA_DIGIT));
            assertEquals(Boolean.FALSE, BytesInternal.parseBoolean(f, StopCharTesters.NON_ALPHA_DIGIT));
            assertEquals(Boolean.TRUE, BytesInternal.parseBoolean(y, StopCharTesters.NON_ALPHA_DIGIT));
            assertEquals(Boolean.FALSE, BytesInternal.parseBoolean(n, StopCharTesters.NON_ALPHA_DIGIT));
            assertEquals(Boolean.TRUE, BytesInternal.parseBoolean(one, StopCharTesters.NON_ALPHA_DIGIT));
            assertEquals(Boolean.FALSE, BytesInternal.parseBoolean(zero, StopCharTesters.NON_ALPHA_DIGIT));
            assertNull(BytesInternal.parseBoolean(maybe, StopCharTesters.NON_ALPHA_DIGIT));
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
            assertEquals("alpha", sb.toString());
            sb.setLength(0);
            BytesInternal.parseUtf8(beta, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("beta", sb.toString());
        } finally {
            alpha.releaseLast();
            beta.releaseLast();
        }
    }
}
