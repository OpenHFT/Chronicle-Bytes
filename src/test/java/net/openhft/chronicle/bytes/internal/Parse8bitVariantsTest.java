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

import static org.junit.Assert.assertEquals;

public class Parse8bitVariantsTest extends BytesTestCommon {

    @Test
    public void parse8bitIntoStringBuilderAndBytes() {
        Bytes<?> alpha = Bytes.from("alpha");
        Bytes<?> beta = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parse8bit(alpha, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString());
            Bytes<?> out = Bytes.allocateElasticOnHeap(8);
            try {
                BytesInternal.parse8bit(beta, out, StopCharTesters.NON_ALPHA_DIGIT);
                assertEquals("beta", out.toString());
            } finally {
                out.releaseLast();
            }
        } finally {
            alpha.releaseLast();
            beta.releaseLast();
        }
    }
}
