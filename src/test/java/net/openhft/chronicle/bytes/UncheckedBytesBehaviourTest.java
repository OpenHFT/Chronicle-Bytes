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
